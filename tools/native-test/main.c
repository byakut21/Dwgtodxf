#include <stdio.h>
#include <string.h>
#include "convert.h"
int main (int argc, char **argv)
{
  if (argc != 4) { fprintf (stderr, "usage: %s dwg2dxf|dxf2dwg in out\n", argv[0]); return 2; }
  int r = strcmp (argv[1], "dwg2dxf") == 0 ? conv_dwg_to_dxf (argv[2], argv[3])
                                           : conv_dxf_to_dwg (argv[2], argv[3]);
  printf ("%s %s -> %s : %d\n", argv[1], argv[2], argv[3], r);
  return r ? 1 : 0;
}
