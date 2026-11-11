/* DWG <-> DXF conversion on top of LibreDWG (GPLv3). */
#include "config.h"
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <dwg.h>
#include "common.h"
#include "bits.h"
#include "out_dxf.h"
#include "convert.h"

int
conv_dwg_to_dxf (const char *in_path, const char *out_path)
{
  Dwg_Data dwg;
  Bit_Chain dat = { 0 };
  int error;

  memset (&dwg, 0, sizeof (Dwg_Data));
  dwg.opts = 0;
  error = dwg_read_file (in_path, &dwg);
  if (error >= DWG_ERR_CRITICAL)
    {
      dwg_free (&dwg);
      return error;
    }
  dat.version = dwg.header.version;
  dat.from_version = dwg.header.from_version;
  unlink (out_path);
  dat.fh = fopen (out_path, "wb");
  if (!dat.fh)
    {
      dwg_free (&dwg);
      return -1;
    }
  error = dwg_write_dxf (&dat, &dwg);
  fclose (dat.fh);
  dwg_free (&dwg);
  return error >= DWG_ERR_CRITICAL ? error : 0;
}

int
conv_dxf_to_dwg (const char *in_path, const char *out_path)
{
  Dwg_Data dwg;
  int error;

  memset (&dwg, 0, sizeof (Dwg_Data));
  dwg.opts = 0;
  /* LibreDWG can only encode R13..R2000 for now: always write AutoCAD 2000. */
  dwg.header.version = R_2000;
  error = dxf_read_file (in_path, &dwg);
  if (error >= DWG_ERR_CRITICAL)
    {
      dwg_free (&dwg);
      return error;
    }
  if (dwg.header.from_version < R_13b1 || dwg.header.from_version >= R_2004)
    dwg.header.version = R_2000;
  if (dwg.header.from_version == R_INVALID)
    dwg.header.from_version = dwg.header.version;
  if (dwg.header.version == R_INVALID)
    dwg.header.version = R_2000;
  unlink (out_path);
  error = dwg_write_file (out_path, &dwg);
  dwg_free (&dwg);
  return error >= DWG_ERR_CRITICAL ? error : 0;
}
