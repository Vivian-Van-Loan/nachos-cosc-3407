#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

int main() {
    int val = 17;
    int* valP = &val;
    char* argv[] = {"XXXX"};
    memcpy(argv[1], &valP, sizeof(int*));
    char* name = "child_deref.coff";
    int pids[10];
    int i;
    for (i = 0; i < 10; i++) {
        pids[i] = exec(name, 1, argv);
        printf("child pid: %d\n", pids[i]);
        if (pids[i] == -1) {
            printf("exec #%d failed\n", i);
            exit(1);
        }
    }

    int childStatus;
    for (i = 0; i < 10; i++) {
        if (join(pids[i], &childStatus) != 0) {
            printf("Joined failed in some form\n");
            exit(1);
        }
        if (childStatus == -1) {
            printf("child #%d exited with %d status\n", i, childStatus);
        }
    }
}
