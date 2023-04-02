#include "stdio.h"
#include "syscall.h"

int main() {
    char* argv[] = {"first"};
//    char* names[] = {"child_exit.coff", "child_exit.coff", "child_exit.coff", "child_exit.coff", "child_exit.coff", "child_exit.coff", "child_exit.coff", "child_exit.coff", "child_exit.coff", "child_exit.coff"};
    char* name = "child_exit.coff";
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
        if (join(pids[i], &childStatus) == -1) {
            printf("Joined failed\n");
            exit(1);
        }
        if (childStatus == -1) {
            printf("child #%d exited with %d status\n", i, childStatus);
        }
    }
}
