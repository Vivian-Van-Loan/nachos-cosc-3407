#include "stdio.h"
#include "syscall.h"

int main() {
    char line[1024];
    printf("Please enter some text (up to 1024 bytes): ");
    readline(line, sizeof(line));
    printf("You entered: %s\n", line);
}
