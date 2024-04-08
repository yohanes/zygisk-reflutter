#include <jni.h>
#include <string>
#include "zygisk.h"
#include <android/log.h>
#include <dlfcn.h>
#include <android/dlext.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <fstream>
#include <sys/mman.h>




#define LOG_TAG "ZygiskReflutter"

void *(*orig__dlopen)(const char *filename, int flags);

void *my_dlopen(const char *filename, int flags)
{
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "my_dlopen: %s", filename);
    return orig__dlopen(filename, flags);
}

static void send_string(int fd, const char *str) {
    int len = 0;
    if (str) {
        len = strlen(str);
    }
    write(fd, &len, sizeof(len));
    write(fd, str, len);
}

static std::string so_replacement;

void *(*orig_android_dlopen_ext)(const char *_Nullable __filename, int __flags, const android_dlextinfo *_Nullable __info);

void *android_dlopen_ext(const char *_Nullable __filename, int __flags, const android_dlextinfo *_Nullable __info)
{
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "android_dlopen_ext: %s flags: %08x", __filename, __flags);

    if (!so_replacement.empty() && so_replacement[0]=='/') {
        //if trying to load libflutter.so
        if (strstr(__filename, "libflutter.so")) {
            __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "android_dlopen_ext replace: %s", so_replacement.c_str());
            void *res = orig_android_dlopen_ext(so_replacement.c_str(), __flags, __info);
            if (!res) {
                __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "android_dlopen_ext replace result2: %p %s", res, dlerror());
                if (!res) {
                    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "failed hooking");
                    //reopen orig
                    res = orig_android_dlopen_ext(__filename, __flags, __info);
                }
            }
            __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "android_dlopen_ext returning %p", res);
            return res;
        }
    }

    return orig_android_dlopen_ext(__filename, __flags, __info);
}

class ZygiskReflutter : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override {
        this->api = api;
        this->env = env;
        do_hook = false;
    }

    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
        auto package_name = env->GetStringUTFChars(args->nice_name, nullptr);
        auto app_data_dir = env->GetStringUTFChars(args->app_data_dir, nullptr);
        preSpecialize(package_name, app_data_dir);
        env->ReleaseStringUTFChars(args->nice_name, package_name);
        env->ReleaseStringUTFChars(args->app_data_dir, app_data_dir);
    }

    void postAppSpecialize(const zygisk::AppSpecializeArgs *) override {
        if (do_hook) {
            //hook dlopen
            api->pltHookRegister(".*", "dlopen", (void *) my_dlopen, (void **) &orig__dlopen);
            //hook android_dlopen_ext
            api->pltHookRegister(".*", "android_dlopen_ext", (void *) android_dlopen_ext, (void **) &orig_android_dlopen_ext);
            api->pltHookCommit();

        }
        if (create_installed) {
            //create a file to indicate that we are installed
            std::string installed_file = "/data/data/com.tinyhack.zygiskreflutter/files/installed.txt";
            std::ofstream out_file(installed_file);
            if (out_file.is_open()) {
                out_file << "installed";
                out_file.close();
            } else {
                __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "failed to create installed file");
            }
        }
    }

private:
    zygisk::Api *api;
    JNIEnv *env;
    bool do_hook;
    bool create_installed;

    void preSpecialize(const char *package_name, const char *app_data_dir) {

            if (strcmp(package_name, "com.tinyhack.zygiskreflutter") == 0) {
                create_installed = true;
                return;
            }

            //__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "preSpecialize: '%s', %s", package_name,  app_data_dir);

            int fd = api->connectCompanion();
            send_string(fd, package_name);
            send_string(fd, app_data_dir);
            //read the path length
            int path_len = 0;
            int len = read(fd, &path_len, sizeof(path_len));
            if (len <= 0) {
                __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "read path length failed %d err = %d", len, errno);
                return;
            }
            //read the path
            char buf[1024];
            len = read(fd, buf, path_len);
            if (len <= 0) {
                __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "read path failed");
                return;
            }
            buf[len] = '\0';
            if (strcmp(buf, "error")==0) {
                return;
            }

            so_replacement = buf;

            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "preSpecialize: ZygiskReflutter: %s", so_replacement.c_str());
            do_hook = true;

    }
};

static std::string readFirstLine(const char *filename) {
    std::ifstream in_file(filename);
    std::string firstLine;
    if (in_file.is_open()) {
        std::getline(in_file, firstLine);
        in_file.close();
    }
    return firstLine;
}

static void writeLine(const char *filename, const char *line) {
    std::ofstream out_file(filename);
    if (out_file.is_open()) {
        out_file << line;
        out_file.close();
    }
}

static void patchIP(const char *so_filename, const char *new_ip) {
    const char *old_ip = "192.168.133.104";
    //patch binary file using mmap
    int fd = open(so_filename, O_RDWR);
    if (fd < 0) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "open file failed: %s", so_filename);
        return;
    }
    struct stat st;
    if (fstat(fd, &st) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "fstat failed: %s", so_filename);
        close(fd);
        return;
    }
    void *addr = mmap(nullptr, st.st_size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (addr == MAP_FAILED) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "mmap failed: %s", so_filename);
        close(fd);
        return;
    }
    //search for old_ip
    char *p = (char *) addr;
    char *end = p + st.st_size;
    while (p < end) {
        if (memcmp(p, old_ip, strlen(old_ip)) == 0) {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "found old ip at %p", p);
            memcpy(p, new_ip, strlen(new_ip));
            break;
        }
        p++;
    }
    munmap(addr, st.st_size);
    close(fd);

}

static std::string read_string(int fd)
{
    int len = 0;
    int r = read(fd, &len, sizeof(len));
    if (r <= 0) {
        return "";
    }
    if (len <= 0) {
        return "";
    }
    char buf[1024];
    read(fd, buf, len);
    buf[len] = '\0';
    return buf;

}

static void my_companion(int fd) {

        std::string package_name = read_string(fd);
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "companion: package %s", package_name.c_str());
        std::string app_data_dir = read_string(fd);
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "companion: datadir %s", app_data_dir.c_str());

        std::string package_info = "/data/data/com.tinyhack.zygiskreflutter/files/" + package_name + ".txt";
        //read the file
        std::string hash = readFirstLine(package_info.c_str());
        if (hash.empty()) {
            send_string(fd, "error");
            return;
        }
        std::string sofile  = "/data/data/com.tinyhack.zygiskreflutter/files/" + hash + ".so";
        //check if exists
        struct stat st;
        if (stat(sofile.c_str(), &st) != 0) {
            __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "companion: %s does not exists", sofile.c_str());
            send_string(fd, "error");
            return;
        }

        std::string ipfile  = "/data/data/com.tinyhack.zygiskreflutter/files/proxyip.txt";
        std::string latest_ip = readFirstLine(ipfile.c_str());

        //get uid/gid of the package dir owner
        if (stat(app_data_dir.c_str(), &st) == 0) {
            __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "companion: %d %d", st.st_uid, st.st_gid);
        }

        std::string dest_dir = app_data_dir +"/files/";
        mkdir(dest_dir.c_str(), 0755);
        chown(dest_dir.c_str(), st.st_uid, st.st_gid);

        std::string package_ip_file = dest_dir + hash + ".txt";
        std::string ip_now = readFirstLine(package_ip_file.c_str());

        std::string dest_file = dest_dir + hash + ".so";

        bool need_copy = false;
        //check if dest file does not exists
        if (stat(dest_file.c_str(), &st)!=0) {
            need_copy = true;
        } else {
            //check if the ip has changed
            if (ip_now != latest_ip) {
                __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "companion: ip changed '%s' '%s'", ip_now.c_str(), latest_ip.c_str());
                need_copy = true;
            }
        }

        if (need_copy) {
            //copy so file
            std::string cmd = "cp " + sofile + " " + dest_file;
            system(cmd.c_str());
            //chown
            chown(dest_file.c_str(), st.st_uid, st.st_gid);
            //chmod 755
            chmod(dest_file.c_str(), 0755);
            //patch the dest file
            patchIP(dest_file.c_str(), latest_ip.c_str());
            //write ip
            writeLine(package_ip_file.c_str(), latest_ip.c_str());
        }

        send_string(fd, dest_file.c_str());

}

REGISTER_ZYGISK_MODULE(ZygiskReflutter)
REGISTER_ZYGISK_COMPANION(my_companion)
