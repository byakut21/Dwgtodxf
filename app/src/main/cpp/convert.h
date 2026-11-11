#ifndef DWGCONV_CONVERT_H
#define DWGCONV_CONVERT_H
/* Return value: 0 = ok, otherwise LibreDWG error bits (>= 0x80 is critical),
   or -1 for an I/O error opening the output. */
int conv_dwg_to_dxf (const char *in_path, const char *out_path);
int conv_dxf_to_dwg (const char *in_path, const char *out_path);
#endif
