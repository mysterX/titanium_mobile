/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;

public class TiNinePatchHelper
{

	class SegmentColor {
		int index;
		int color;
	};

	public Drawable process(Drawable d) {
		Drawable nd = d;

		if (d instanceof BitmapDrawable) {
			Bitmap b = ((BitmapDrawable) d).getBitmap();
			if (b != null) {
				if (isNinePatch(b)) {
			        byte[] newChunk = createChunk(b);
			        nd = new NinePatchDrawable(cropNinePatch(b), newChunk, new Rect(1,1,1,1), "");
				}
			}
		}

		return nd;
	}

	public Drawable process(Bitmap b)
	{
		Drawable nd = null;

		if (b != null) {
			if (isNinePatch(b)) {
		        byte[] newChunk = createChunk(b);
		        nd = new NinePatchDrawable(cropNinePatch(b), newChunk, new Rect(1,1,1,1), "");
			} else {
				nd = new BitmapDrawable(b);
			}
		}

		return nd;
	}

	private boolean isNinePatch(Bitmap b) {
		// NinePatch image must have a one-pixel transparent border added around the normal image
		// and one-pixel black lines at the top and left to define the stretch section.
		// NinePatch image may have one-pixel black lines at the bottom and right to define the
		// drawable section.
		if (!b.hasAlpha()) {
			return false;
		}

		boolean result = true;

		int width = b.getWidth();
		int height = b.getHeight();

		int topSum = 0;
		int leftSum = 0;
		int rightSum = 0;
		int bottomSum = 0;

		if (width >= 4 && height >= 4) {
			for (int i = 0; i < width; i++) {
				if (b.getPixel(i, 1) != 0) { // The second top pixels have to be transparent.
					result = false;
					break;
				}

				int c = b.getPixel(i, 0);
				topSum += (c == 0 ? 0 : 1);
				if (!isValidColor(c)) {
					result = false;
					break;
				}

				c = b.getPixel(i, height-1);
				bottomSum += (c == 0 ? 0 : 1);
				if (!isValidColor(c)) {
					result = false;
					break;
				}
			}

			if (result) {
				for (int i = 0; i < height; i++) {
					if (b.getPixel(1, i) != 0) { // The second left pixels have to be transparent.
						result = false;
						break;
					}

					int c = b.getPixel(0, i);
					leftSum += (c == 0 ? 0 : 1);
					if (!isValidColor(c)) {
						result = false;
						break;
					}

					c = b.getPixel(width-1, i);
					rightSum += (c == 0 ? 0 : 1);
					if (!isValidColor(c)) {
						result = false;
						break;
					}
				}
			}
		} else {
			result = false;
		}

		// Don't consider a transparent border a ninepatch
		if (leftSum + topSum + rightSum + bottomSum == 0) {
			result = false;
		}

		return result;
	}

	private boolean isValidColor(int c) {
		return (c == 0 || (c == Color.BLACK));
	}

    private Bitmap cropNinePatch(Bitmap b) {
    	Bitmap cb = null;

    	cb = Bitmap.createBitmap(b.getWidth()-2, b.getHeight()-2, b.getConfig());
    	int[] pixels = new int[cb.getWidth() * cb.getHeight()];
    	b.getPixels(pixels, 0, cb.getWidth(), 1, 1, cb.getWidth(), cb.getHeight());
    	cb.setPixels(pixels, 0, cb.getWidth(), 0, 0, cb.getWidth(), cb.getHeight());

    	return cb;
    }

    byte[] createChunk(Bitmap b) {
    	byte[] chunk = null;

    	int numXDivs = 0;
    	int numYDivs = 0;
    	int numColors = 1;

    	int last = b.getPixel(0, 0);

    	ArrayList<SegmentColor> xdivs = new ArrayList<SegmentColor>();
    	// Walk Top
    	for (int x = 1; x < b.getWidth(); x++) {
    		int p = b.getPixel(x, 0);
    		if (p != last) {
    			SegmentColor sc = new SegmentColor();
    			sc.index = x;
    			sc.color = last;
    			xdivs.add(sc);
    			numXDivs++;
    			last = p;
    		}
    	}

    	last = b.getPixel(0, 0);

    	// Walk left
    	ArrayList<SegmentColor> ydivs = new ArrayList<SegmentColor>();
    	for (int y = 1; y < b.getHeight(); y++) {
    		int p = b.getPixel(0, y);
    		if (p != last) {
    			SegmentColor sc = new SegmentColor();
    			sc.index = y;
    			sc.color = last;
    			ydivs.add(sc);
    			numYDivs++;
    			last = p;
    		}
    	}

    	// Calculate Region Colors
    	ArrayList<Integer> colors =  new ArrayList<Integer>();
    	for (int y = 0; y < ydivs.size(); y++) {
    		int yc = ydivs.get(y).color;
    		for (int x = 0; x < xdivs.size(); x++) {
    			if (yc == 0) {
    				colors.add(xdivs.get(x).color == 0 ? 0 : 1);
    			} else {
    				colors.add(ydivs.get(y).color == 0 ? 0 : 1);
    			}
    		}
    		if (yc == 0) {
    			colors.add(colors.get(colors.size()-1) == 1 ? 0 : 1);
    		} else {
    			colors.add(1);
    		}
    	}
    	for (int i = 0; i < xdivs.size()+1; i++) {
    		colors.add(colors.get(i));
    	}

    	numColors = colors.size();

    	// Figure out the size / looks like padded to 32bits.
    	int size = 32 + // wasDeserialized, numXDivs, numYDivs, numColors, padLeft, padRight, padTop, padBottom
    		numXDivs * 32 +
    		numYDivs * 32 +
    		numColors * 32
    		;

    	chunk = new byte[size];
    	chunk[0] = 0;
    	chunk[1] = (byte) (0xFF & numXDivs);
    	chunk[2] = (byte) (0xFF & numYDivs);
    	chunk[3] = (byte) (0xFF & numColors);

    	int startOfXData = 32;
    	for(int i = 0; i < xdivs.size(); i++) {
    		toBytes(chunk, startOfXData + (i * 4), xdivs.get(i).index-1);
    	}

    	int startOfYData = startOfXData + (numXDivs * 4);
    	for(int i = 0; i < ydivs.size(); i++) {
    		toBytes(chunk, startOfYData + (i * 4), ydivs.get(i).index-1);
    	}

    	int startOfColors = startOfYData + (numYDivs * 4);
    	for(int i = 0; i < colors.size(); i++) {
    		toBytes(chunk, startOfColors + (i * 4), 1/*colors.get(i)*/);
    	}

    	return chunk;
    }

    private int toInt(byte[] a, int offset) {
    	int i = 0;

    	i |= a[offset];
    	i |= a[offset+1] << 8;
    	i |= a[offset+2] << 16;
    	i |= a[offset+3] << 24;

    	return i;
    }

    private void toBytes(byte[] a, int offset, int v) {
    	a[offset] = (byte) (0x000000FF & v);
    	a[offset+1] = (byte) ((0x0000FF00 & v) >> 8);
    	a[offset+2] = (byte) ((0x00FF0000 & v) >> 16);
    	a[offset+3] = (byte) ((0xFF000000 & v) >> 24);
    }


}
