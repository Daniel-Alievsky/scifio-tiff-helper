/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.scifio.tiff.helper;

import io.scif.FormatException;
import io.scif.formats.tiff.IFD;
import io.scif.formats.tiff.IFDType;
import io.scif.formats.tiff.TiffSaver;
import io.scif.io.RandomAccessOutputStream;
import org.scijava.Context;

import java.io.IOException;

class TiffSaverWith32BitGeometricTags extends TiffSaver {

    public TiffSaverWith32BitGeometricTags(Context context, String filename) throws IOException {
        super(context, filename);
    }

    @Override
    public void writeIFDValue(RandomAccessOutputStream extraOut, long offset, int tag, Object value)
        throws FormatException, IOException
    {
        if (value instanceof Long && isBigTiff()) {
            final long v = (Long) value;
            if (v == (int) v) {
                // - it is very probable for the following tags while using via SequentialTiffWriter
                switch (tag) {
                    case IFD.IMAGE_WIDTH:
                    case IFD.IMAGE_LENGTH:
                    case IFD.TILE_WIDTH:
                    case IFD.TILE_LENGTH:
                    case IFD.ROWS_PER_STRIP: {
                        final RandomAccessOutputStream out = getStream();
                        out.writeShort(tag);
                        out.writeShort(IFDType.LONG.getCode());
                        // - TiffSaver class saves LONG8 in this case, but some programs
                        // (like Aperio Image Viewer) do not understand such values
                        out.writeLong(1L);
                        out.writeInt((int) v);
                        out.writeInt(0);
                        // - 4 bytes of padding until full length 20 bytes
                        return;
                    }
                }
            }
        }
        super.writeIFDValue(extraOut, offset, tag, value);
    }
}
