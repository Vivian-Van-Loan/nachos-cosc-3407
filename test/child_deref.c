#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

int main(int argc, char* argv[]) {
    int* intPointer;
    memcpy(&intPointer, argv[0], sizeof(intPointer));
    printf("points to: %d\n", *intPointer);
}
