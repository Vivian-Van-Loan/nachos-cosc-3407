#include "stdio.h"
#include "syscall.h"

int main() {
    int fd = creat("wuc.bin");
    if (fd == -1) {
        printf("Failed to create wuc.bin\n");
        exit(1);
    }
    int written = write(fd, "Hello\n", 6);
    if (written == -1) {
        printf("Failed to write\n");
        exit(1);
    }
    int unlinkRet = unlink("wuc.bin");
    if (unlinkRet == -1) {
        printf("Failed to unlink\n");
        exit(1);
    }
    int closeRet = close(fd);
    if (closeRet == -1) {
        printf("Failed to close\n");
        exit(1);
    }
    fd = open("wuc.bin");
    if (fd != -1) {
        printf("Failed: able to open file again, delayed unlink failed\n");
        exit(1);
    }
}
