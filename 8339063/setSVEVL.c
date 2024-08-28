#include <linux/prctl.h>
#include <sys/prctl.h>
#include <stdio.h>
#include <errno.h>

int main(void) {
  for (int i = 0; i < 16; i++) {
    printf("Configure to %d\n", i);
    int retval = prctl(PR_SVE_SET_VL, i);
    printf("%d %d\n", retval, errno);
    errno = 0;
  }
  for (int i = 16; i <= 256; i += 16) {
    printf("Configure to %d\n", i);
    int retval = prctl(PR_SVE_SET_VL, i);
    printf("%d %d\n", retval, errno);
    errno = 0;
  }
}

