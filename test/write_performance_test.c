#include "stdio.h"
#include "syscall.h"

char loremIpsum[] = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Dui nunc mattis enim ut tellus elementum sagittis vitae. Mattis ullamcorper velit sed ullamcorper morbi tincidunt ornare massa. Sed viverra ipsum nunc aliquet bibendum enim facilisis. Ullamcorper malesuada proin libero nunc consequat interdum. Quis enim lobortis scelerisque fermentum dui faucibus. Feugiat in fermentum posuere urna. Sed viverra ipsum nunc aliquet bibendum. Sodales neque sodales ut etiam sit amet nisl. Id ornare arcu odio ut sem nulla pharetra diam sit. Pulvinar etiam non quam lacus suspendisse faucibus. Urna neque viverra justo nec. Libero volutpat sed cras ornare. Orci eu lobortis elementum nibh tellus molestie. Enim eu turpis egestas pretium aenean pharetra magna ac. Lacinia at quis risus sed. Varius quam quisque id diam vel. At tellus at urna condimentum mattis pellentesque. Neque aliquam vestibulum morbi blandit. Fermentum odio eu feugiat pretium nibh ipsum consequat nisl vel. Nisl pretium fusce id velit. Cras ornare arcu dui vivamus arcu. Magna etiam tempor orci eu lobortis elementum nibh tellus. Elementum pulvinar etiam non quam lacus suspendisse faucibus. Pellentesque id nibh tortor id aliquet lectus proin. Semper risus in hendrerit gravida rutrum quisque non tellus orci. Ornare massa eget egestas purus viverra accumsan. Ut porttitor leo a diam sollicitudin tempor. Quis ipsum suspendisse ultrices gravida dictum fusce. Nisi porta lorem mollis aliquam ut porttitor leo a. Purus viverra accumsan in nisl nisi scelerisque. Orci eu lobortis elementum nibh tellus molestie nunc non. Posuere urna nec tincidunt praesent semper feugiat. Aliquet lectus proin nibh nisl condimentum id venenatis a. Integer malesuada nunc vel risus commodo viverra maecenas. Risus nec feugiat in fermentum posuere urna nec tincidunt. Porttitor eget dolor morbi non arcu. Commodo quis imperdiet massa tincidunt nunc pulvinar sapien. Ullamcorper sit amet risus nullam eget felis eget. Scelerisque fermentum dui faucibus in ornare quam viverra orci. Aliquet eget sit amet tellus cras. Ut aliquam purus sit amet luctus venenatis lectus. Ut porttitor leo a diam sollicitudin tempor id eu nisl. Pharetra et ultrices neque ornare aenean euismod elementum. Suspendisse sed nisi lacus sed viverra tellus. Vitae proin sagittis nisl rhoncus mattis rhoncus. Cras pulvinar mattis nunc sed blandit libero. Amet dictum sit amet justo donec enim diam vulputate. Purus non enim praesent elementum facilisis leo vel fringilla. Sed elementum tempus egestas sed sed risus. Luctus accumsan tortor posuere ac ut consequat. Nec feugiat nisl pretium fusce id velit ut tortor pretium. Eget aliquet nibh praesent tristique magna. Ac odio tempor orci dapibus ultrices in iaculis nunc. Fames ac turpis egestas integer eget aliquet. Phasellus faucibus scelerisque eleifend donec pretium vulputate sapien nec sagittis. Sem nulla pharetra diam sit amet nisl suscipit. Praesent semper feugiat nibh sed. Ut faucibus pulvinar elementum integer. Maecenas volutpat blandit aliquam etiam erat velit scelerisque in. Feugiat vivamus at augue eget arcu dictum varius duis. Tellus pellentesque eu tincidunt tortor aliquam nulla facilisi. Pharetra sit amet aliquam id diam maecenas. Consequat interdum varius sit amet. Laoreet sit amet cursus sit. In pellentesque massa placerat duis ultricies. Vitae proin sagittis nisl rhoncus mattis rhoncus. In nisl nisi scelerisque eu ultrices vitae auctor. Tempus urna et pharetra pharetra massa. Quam quisque id diam vel quam elementum pulvinar etiam. Vitae congue mauris rhoncus aenean vel.\n";
char loremIpsumRead[sizeof(loremIpsum)] = {0};

//Time and collect memory use of this one with a Java profiler
int main() {
    int fd = creat("perftest.bin");
    if (fd == -1) {
        printf("Failed to creat perftest.bin\n");
        exit(1);
    }

    int i;
    for (i = 0; i < 32; i++) {
        int written = write(fd, loremIpsum, sizeof(loremIpsum) - 1);
        if (written != (sizeof(loremIpsum) - 1)) {
            printf("Failed to write all of loremIpsum, only wrote: %d bytes\n", written);
            exit(1);
        }
        int readIn = read(fd, loremIpsumRead, sizeof(loremIpsumRead) - 1);
        if (readIn == -1) {
            printf("Failed to read in loremIpsumRead\n", readIn);
            exit(1);
        }
    }


    int closeRet = close(fd);
    if (closeRet == -1) {
        printf("Failed to close\n");
        exit(1);
    }
    int unlinkRet = unlink("perftest.bin");
    if (unlinkRet == -1) {
        printf("Failed to unlink\n");
        exit(1);
    }
}
