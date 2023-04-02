#include "stdio.h"
#include "syscall.h"

int main() {
    char* legalArgv[] = {"first", "second"}; //these should be printed to stdout
    char* legalName = "echo.coff";
    int legalPID = exec(legalName, 2, legalArgv);
    if (legalPID == -1) {
        printf("exec failed\n");
        exit(1);
    }
    char illegalName = "wagabagabooboo.coff";
    int illegalPID = exec(illegalName, 2, legalArgv);
    if (illegalPID != -1) {
        printf("Failed: Exec with illegal name ran something\n");
        exit(1);
    }
    int childStatus;
    if (join(legalPID, &childStatus) == -1) {
        printf("Joined failed\n");
        exit(1);
    }
    printf("Exit status: %d\n", childStatus);
}
