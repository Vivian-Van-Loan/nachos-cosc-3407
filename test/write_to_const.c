#include "syscall.h"
#include "stdio.h"

char const str[] = "Hello!\n";

int main() {
    str[0] = "W";
}
