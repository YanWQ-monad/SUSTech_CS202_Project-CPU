import png
import os
import struct


def load_glyph_data_from_png(file_path):
    width, height, bitmap, info = png.Reader(filename=file_path).read()
    glyph_data = []
    for bitmap_row in bitmap:
        glyph_data_row = []
        bitmap_row_len = len(bitmap_row)
        pixel_step = int(bitmap_row_len / width)
        for x in range(pixel_step - 1, bitmap_row_len, pixel_step):
            alpha = bitmap_row[x]
            if alpha > 127:
                glyph_data_row.append(1)
            else:
                glyph_data_row.append(0)
        glyph_data.append(glyph_data_row)
    return glyph_data, width, height


if __name__ == '__main__':
    # for i in range(0x20, 0x7F):
    #     (data, _, _) = load_glyph_data_from_png(f'0000-007F Basic Latin/00{i:02X}.png')
    #     with open(f'font_data/{i:02X}.txt', 'w') as f:
    #         data = '\n'.join([ ''.join([ '_' if c == 0 else '*' for c in row ]) for row in data ])
    #         f.write(data + '\n')

    with open('font.txt', 'w') as f:
        for i in range(0x00, 0xFF + 1):
            filename = f'font_data/{i:02X}.txt'
            if os.path.exists(filename):
                with open(f'font_data/{i:02X}.txt') as g:
                    lines = [ line.strip() for line in g ]
                values = [ int(line.replace('_', '0').replace('*', '1')[::-1], 2) for line in lines ]
            else:
                values = [ 0 ] * 16
            for v in values:
                f.write(struct.pack('>B', v).hex() + '\n')
