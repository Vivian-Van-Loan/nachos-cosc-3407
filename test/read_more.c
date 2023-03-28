#include "stdio.h"
#include "syscall.h"

char hello[] = "Hello!\n";

int main() {
    int fd = creat("read_more.bin");
    if (fd == -1) {
        printf("Failed to create read_more.bin\n");
        exit(1);
    }
    int written = write(fd, hello, sizeof(hello) - 1);
    if (written == -1) {
        printf("failed to write\n");
        exit(1);
    }
    int closeRet = close(fd);
    if (closeRet == -1) {
        printf("Failed to close\n");
        exit(1);
    }
    fd = open("read_more.bin");
    if (fd == -1) {
        printf("Failed to reopen file for reading\n");
        exit(1);
    }
    char buf[128];
    int readRet = read(fd, buf, sizeof(buf));
    if (readRet != sizeof(hello) - 1) {
        printf("Failed to read the correct amount from file\n");
        exit(1);
    }
    int unlinkRet = unlink("read_more.bin");
    if (unlinkRet == -1) {
        printf("Failed to unlink\n");
        exit(1);
    }
}