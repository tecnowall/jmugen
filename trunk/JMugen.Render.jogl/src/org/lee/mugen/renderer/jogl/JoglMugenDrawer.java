package org.lee.mugen.renderer.jogl;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.media.opengl.GL;

import org.lee.mugen.imageIO.ImageUtils;
import org.lee.mugen.imageIO.PCXLoader;
import org.lee.mugen.imageIO.PCXPalette;
import org.lee.mugen.imageIO.RawPCXImage;
import org.lee.mugen.imageIO.PCXLoader.PCXHeader;
import org.lee.mugen.renderer.AngleDrawProperties;
import org.lee.mugen.renderer.DrawProperties;
import org.lee.mugen.renderer.GameWindow;
import org.lee.mugen.renderer.ImageContainer;
import org.lee.mugen.renderer.MugenDrawer;
import org.lee.mugen.renderer.RGB;
import org.lee.mugen.renderer.Trans;
import org.lee.mugen.renderer.jogl.shader.AfterImageShader;
import org.lee.mugen.renderer.jogl.shader.PalFxShader;
import org.lee.mugen.util.Logger;

import com.sun.opengl.util.j2d.TextureRenderer;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;

public class JoglMugenDrawer extends MugenDrawer {
	private JoglGameWindow gameWindow = new JoglGameWindow();
	
	//
	private RGB rgba = new RGB();
	
	private static PalFxShader palFxShader;
	private static AfterImageShader afterImageShader;
	
	//
	
	public PalFxShader getPalFxShader() {
		if (palFxShader == null) {
			palFxShader = new PalFxShader();
			palFxShader.compileShader(gameWindow.getGl());
		}
		return palFxShader;
	}
	
	public AfterImageShader getAfterImageShader() {
		if (afterImageShader == null) {
			afterImageShader = new AfterImageShader();
			afterImageShader.compileShader(gameWindow.getGl());
		}
		return afterImageShader;
	}

	private boolean isScaleByForMeDebug() {
		return false;
	}
	
	private void drawImage(float xlDst, float xrDst, float ytDst, float ybDst,
			float xlSrc, float xrSrc, float ytSrc, float ybSrc,
			DrawProperties dp) {
		GL gl = getGl();
		if (gl == null)
			return;
		float xScale = 1f;
		float yScale = 1f;

		if (dp.getAngleDrawProperties() != null) {
			xScale = dp.getAngleDrawProperties().getXScale();
			yScale = dp.getAngleDrawProperties().getYScale();
		}

		// draw a quad textured to match the sprite
		Texture texture = (Texture) dp.getIc().getImg();
		TextureCoords coords = texture.getImageTexCoords();
		gl.glBegin(GL.GL_QUADS);
		{
			
			gl.glNormal3f(0.0f, 0.0f, 1.0f);
			// Left Bottom
//			gl.glTexCoord2f(coords.left(), coords.top());
			gl.glTexCoord2f(xlSrc/texture.getWidth(), ytSrc/texture.getHeight());
			// gl.glVertex2f(0, 0);
			gl.glVertex2f(xlDst, ytDst);

			// Left Top

//			gl.glTexCoord2f(coords.left(), coords.bottom());
			gl.glTexCoord2f(xlSrc/texture.getWidth(), ybSrc/texture.getHeight());
			gl.glVertex2f(xlDst, (ybDst - ytDst) * dp.getYScaleFactor()
					* yScale + ytDst);

			// Right Top
//			gl.glTexCoord2f(coords.right(), coords.bottom());
			gl.glTexCoord2f(xrSrc/texture.getWidth(), ybSrc/texture.getHeight());
			gl.glVertex2f((xrDst - xlDst) * dp.getXScaleFactor() * xScale
					+ xlDst, (ybDst - ytDst) * dp.getYScaleFactor() * yScale
					+ ytDst);

			// Right Bottom
//			gl.glTexCoord2f(coords.right(), coords.top());
			gl.glTexCoord2f(xrSrc/texture.getWidth(), ytSrc/texture.getHeight());
			gl.glVertex2f((xrDst - xlDst) * dp.getXScaleFactor() * xScale
					+ xlDst, ytDst);
		}
		gl.glEnd();
	}
	
	private void drawWithPropertiesColor(DrawProperties dp) {
		GL gl = getGl();
		if (gl == null)
			return;
		Texture texture = (Texture) dp.getIc().getImg();
		float xlDst = dp.getXLeftDst();
		float xrDst = dp.getXRightDst();
		float ytDst = dp.getYTopDst();
		float ybDst = dp.getYBottomDst();

		float xlSrc = (dp.getXLeftSrc() / texture.getImageWidth())
				* texture.getWidth();
		float xrSrc = (dp.getXRightSrc() / texture.getImageWidth())
				* texture.getWidth();

		float ytSrc = (dp.getYTopSrc() / texture.getImageHeight())
				* texture.getHeight();
		float ybSrc = (dp.getYBottomSrc() / texture.getImageHeight())
				* texture.getHeight();

		ImageContainer ic = dp.getIc();
		if (dp.getTrans() == Trans.ADD1) {
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_DST_ALPHA);
		} else if (dp.getTrans() == Trans.ADD) {
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_DST_ALPHA);
		} else if (dp.getTrans() == Trans.ADDALPHA) {
			gl.glBlendFunc(GL.GL_SRC_ALPHA,
					GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glColor4f(1f, 1f, 1f, 0.5f);
		}
		if (dp.getPalfx() != null) {

			float alpha = (float) (Math.PI * dp.getPalfx().getTimeActivate() / dp
					.getPalfx().getSinadd().getPeriod());

			int rPlus = (int) (dp.getPalfx().getSinadd().getAmpl_r() * Math
					.sin(2 * alpha));
			int gPlus = (int) (dp.getPalfx().getSinadd().getAmpl_g() * Math
					.sin(2 * alpha));
			int bPlus = (int) (dp.getPalfx().getSinadd().getAmpl_b() * Math
					.sin(2 * alpha));

			RGB ampl = new RGB(rPlus, gPlus, bPlus, 255f);
			RGB bits = new RGB(1f/255f, 1f/255f, 1f/255f, 1f/255f);
			getPalFxShader().render(gl,
					dp.getPalfx().getAdd().mul(bits),
					dp.getPalfx().getMul().mul(bits),
					ampl.mul(bits));
			drawImage(xlDst, xrDst, ytDst, ybDst, xlSrc, xrSrc, ytSrc, ybSrc,
					dp);
			getPalFxShader().endRender(gl);

		} else if (dp.getImageProperties() != null) {
			RGB bits = new RGB(1f/255f, 1f/255f, 1f/255f, 1f/255f);
			getAfterImageShader().render(gl,
					dp.getImageProperties().getPalbright().mul(bits), 
					dp.getImageProperties().getPalcontrast().mul(bits), 
					dp.getImageProperties().getPalpostbright().mul(bits),
					dp.getImageProperties().getPaladd().mul(bits),
					dp.getImageProperties().getPalmul()
			
				);
			drawImage(xlDst, xrDst, ytDst, ybDst, xlSrc, xrSrc, ytSrc, ybSrc, dp);
			getAfterImageShader().endRender(gl);			
			
		} else {

			gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE,
					GL.GL_MODULATE);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

			if (dp.getTrans() == Trans.ADD1) {
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_DST_ALPHA);
			} else if (dp.getTrans() == Trans.ADD) {
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_DST_ALPHA);
			} else if (dp.getTrans() == Trans.ADDALPHA) {
				gl.glBlendFunc(GL.GL_SRC_ALPHA,
						GL.GL_ONE_MINUS_SRC_ALPHA);
				gl.glColor4f(1f, 1f, 1f, 0.5f);
			}
			drawImage(xlDst, xrDst, ytDst, ybDst, xlSrc, xrSrc, ytSrc, ybSrc, dp);
		}

		gl.glDisable(GL.GL_ALPHA_TEST);
	}
	
	
	private void processRotationProperties(AngleDrawProperties dp) {
		GL gl = getGl();
		if (gl == null)
			return;
		if (dp != null) {
			gl.glTranslatef(dp.getXAnchor(), dp.getYAnchor(), 0);
			gl.glRotatef(dp.getAngleset(), 0, 0, 1);
			gl.glTranslatef(-dp.getXAnchor(), -dp.getYAnchor(), 0);

		}
	}
	
	///
	
	@Override
	public GameWindow getInstanceOfGameWindow() {
		return gameWindow;
	}
	
	private GL getGl() {
		return gameWindow.getGl();
	}

	TextureRenderer animRenderer = null;
	
	@Override
	public void draw(DrawProperties dp) {
		GL gl = getGl();
		if (gl == null)
			return;
		

		
		
		Texture texture = (Texture) dp.getIc().getImg();

		// store the current model matrix
		gl.glPushMatrix();

		// bind to the appropriate texture for this sprite
		texture.enable();
		texture.bind();
		
		gl.glEnable(GL.GL_TEXTURE_2D);

		gl.glColorMask(true, true, true, true);
		gl.glDisable(GL.GL_COLOR_LOGIC_OP);
		gl.glDisable(GL.GL_BLEND);
//		gl.glDisable(GL.GL_ALPHA_TEST);

		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
//		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
		
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_COLOR, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		
		
		if (isScaleByForMeDebug()) {
			scale(0.5f, 0.5f); // This scale help me to see out of screen
			gl.glTranslated(160, 240, 0);
		}

		
		processRotationProperties(dp.getAngleDrawProperties());

		drawWithPropertiesColor(dp);
		gl.glPopMatrix();
		texture.disable();
	

	}
	
	
	@Override
	public void drawLine(int x, int y, int x2, int y2) {
		GL gl = getGl();
		if (gl == null)
			return;		
	}

	@Override
	public void drawRect(float x, float y, float width, float height) {
		GL gl = getGl();
		if (gl == null)
			return;
		
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glVertex2f(x, y);
		gl.glVertex2f(x + width, y);
		gl.glVertex2f(x + width, y + height);
		gl.glVertex2f(x, y + height);
		gl.glVertex2f(x, y);
		gl.glEnd();
		gl.glEnable(GL.GL_TEXTURE_2D);
	}

	@Override
	public void fillRect(float x1, float y1, float width, float height) {
		GL gl = getGl();
		if (gl == null)
			return;
		
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glColor4f(rgba.getA(), rgba.getG(), rgba.getB(), rgba.getA());

		gl.glBegin(GL.GL_QUADS);
		gl.glVertex2f(x1, y1);
		gl.glVertex2f(x1 + width, y1);
		gl.glVertex2f(x1 + width, y1 + height);
		gl.glVertex2f(x1, y1 + height);
		gl.glEnd();
		gl.glEnable(GL.GL_TEXTURE_2D);
	}

//	@Override
//	public ImageContainer getImageContainer(Object imageData) {
//		RawPCXImage pcx = (RawPCXImage) imageData;
//		BufferedImage image = null;
//		try {
//			image = (BufferedImage) PCXLoader.loadImageColorIndexed(new ByteArrayInputStream(pcx.getData()), pcx.getPalette(), false, true);
//		} catch (IOException e) {
//			throw new IllegalArgumentException();
//		}
//
//		Texture texture = TextureIO.newTexture(image, true);
//
//		ImageContainer ic = new ImageContainer(texture, image.getWidth(), image.getHeight());
//		return ic;
//	}



	@Override
	public void scale(float x, float y) {
		GL gl = getGl();
		if (gl == null)
			return;
		gl.glScaled(x, y, 0);
	}

	@Override
	public void setColor(float r, float g, float b) {
		setColor(r, g, b, 1f);
		
	}

	@Override
	public void setColor(float r, float g, float b, float a) {
		rgba.setA(a);
		rgba.setR(r);
		rgba.setG(g);
		rgba.setB(b);
	}

	

	public class ImageContainerText extends ImageContainer {

		public ImageContainerText(Object img, int width, int height) {
			super(img, width, height);
		}
		
		
		private static final int RAW_PCX = 0;
		private static final int DATA = 1;
		private static final int TEXTURE = 2;
		
		AtomicInteger imageStatus = new AtomicInteger(0);
		@Override
		public Object getImg() {
			synchronized (this) {
				if (imageStatus.get() == TEXTURE) {
					return img;
				} else if (imageStatus.get() == DATA) {
					try {
						
						Texture texture = TextureIO.newTexture((TextureData)img);
						texture.setTexParameteri(GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
						texture.setTexParameteri(GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
						texture.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
						texture.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
						img = texture;
						imageStatus.set(TEXTURE);
						return img;
					} catch (Exception e) {
						throw new IllegalStateException("Ca ne devrait pas arrive", e);
					}
				} else if (imageStatus.get() == RAW_PCX) {
					RawPCXImage pcx = (RawPCXImage) img;
					try {
						BufferedImage image = (BufferedImage) PCXLoader.loadImageColorIndexed(new ByteArrayInputStream(
								pcx.getData()), pcx.getPalette(), false, true);

						Texture texture = TextureIO.newTexture((BufferedImage)image, true);
						texture.setTexParameteri(GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
						texture.setTexParameteri(GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
						texture.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
						texture.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
						img = texture;
						imageStatus.set(TEXTURE);
						return img;
					} catch (IOException e) {
						throw new IllegalStateException("Ca ne devrait pas arrive");
					}
				}
			}
			throw new IllegalStateException();
		}

		public void free() {
			synchronized (this) {
				if (imageStatus.get() == TEXTURE && img != null)
					((Texture)img).dispose();
			}
		}
		
		@Override
		public void reload(ImageContainer img) {
			synchronized (this) {
				ImageContainerText imgText = (ImageContainerText) img;
				this.img = imgText.img;
				this.width = img.getWidth();
				this.height = img.getHeight();
				imageStatus.set(imgText.imageStatus.get());
			}

		}
		
		public void prepareImageToTexture() {
			synchronized (this) {
				if (imageStatus.get() == RAW_PCX) {
					RawPCXImage pcx = (RawPCXImage) img;
					
					try {
						BufferedImage image = pcxToRawRGBA(pcx.getData());
						TextureData data = TextureIO.newTextureData(image, true);
						img = data;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					imageStatus.set(DATA);
					

				}
			}
		}
		
	}
	static ColorModel glAlphaColorModel = new ComponentColorModel(ColorSpace
			.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 }, true,
			false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
	public static BufferedImage pcxToRawRGBA(byte[] data) throws IOException {
    	BufferedImage image;

    	PCXPalette pal = new PCXPalette();
    	PCXHeader header = new PCXHeader(data);
       	pal.load(new ByteArrayInputStream(data));
        
        InputStream in = new ByteArrayInputStream(data);
        
        in.skip(128);
        int width = header.xmax - header.xmin + 1;
        int height = header.ymax - header.ymin + 1;
        
        int xp = 0;
        int yp = 0;
        int value;
        int count;
        
        int texWidth = 2;
        int texHeight = 2;
        
        // find the closest power of 2 for the width and height
        // of the produced texture
        while (texWidth < width) {
            texWidth *= 2;
        }
        while (texHeight < height) {
            texHeight *= 2;
        }

        WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, texWidth, texHeight, 4, null);
        image = new BufferedImage(glAlphaColorModel, raster, false, new Hashtable());
        
        while (yp < height) {
            value = in.read();
            // if the byte has the top two bits set
            if (value >= 192) {
                count = (value - 192);
                value = in.read();
            } else {
                count = 1;
            }
            
            // update data
            for (int i = 0; i < count; i++) {
                if (xp < width) {
                	int[] alpha = new int[] {255};

                     
                	if (
                	isAboutTheSameColor(
                             pal.r[value],
                             pal.g[value],
                             pal.b[value], 
                             pal.r[0], 
                             pal.g[0], 
                             pal.b[0], 
                             alpha)) {
//                		
                	} else {
                        int color = ImageUtils.getARGB(
                                alpha[0],
                                pal.r[value],
                                pal.g[value],
                                pal.b[value]);
                        image.setRGB(xp, yp, color);
                		
                	}

//                    texinfo.texels[xp + yp * width] = ImageUtils.getARGB(alpha[0], pal.r[value], pal.g[value], pal.b[value]);
//                    pal.r[value];
//                    texinfo.texels[xp * 4 + yp * width + 1] = pal.g[value];
//                    texinfo.texels[xp * 4 + yp * width + 2] = pal.b[value];
//                    texinfo.texels[xp * 4 + yp * width + 3] = alpha[0];

                    
                    // TODO Find a way to load it directly in a byte
                }
                xp++;
                if (xp == header.bytesPerLine) {
                    xp = 0;
                    yp ++;
                    break;
                }
            }
        }
        in.close();
        return image; 
	}
	
	private static boolean isAboutTheSameColor(int r1, int g1, int b1, int r2, int g2, int b2, int[] alpha) {
        alpha[0] = 255;
        if (r1 == r2 && b1 == b2 && g1 == g2) {
            alpha[0] = 0;
            return true;
        } else
            return false;
    }	
	
	
	//////////////////////
	private static final int LIST_IMG_TO_PROCESS_COUNT = 4;
	private static final int LIST_IMG_TO_PROCESS_THREAD_YELD_TIME = 100;
	private static List<ImageContainerText>[] IMAGE_TO_PROCESS_LIST = null;
	private static boolean[] jobFinish = new boolean[LIST_IMG_TO_PROCESS_COUNT];
	private static int currentListToAdd = 0;
	
	
	private static void addToImageToProcess(ImageContainerText img) {
		if (IMAGE_TO_PROCESS_LIST == null) {
			IMAGE_TO_PROCESS_LIST = new List[LIST_IMG_TO_PROCESS_COUNT];
			for (int i = 0; i < LIST_IMG_TO_PROCESS_COUNT; i++) {
				IMAGE_TO_PROCESS_LIST[i] = new LinkedList<ImageContainerText>();
			}
		}
		if (currentListToAdd > IMAGE_TO_PROCESS_LIST.length - 1) {
			currentListToAdd = 0;
		}
		IMAGE_TO_PROCESS_LIST[currentListToAdd].add(img);
		currentListToAdd++;
	}
	

	private static void prepareImageToProcess(List<ImageContainerText> list) {
		Collections.sort(list, IMAGE_CONTAINER_COMPARATOR);
		for (Iterator<ImageContainerText> iter = list.iterator(); iter.hasNext();) {
			iter.next().prepareImageToTexture();
			iter.remove();
			try {
				Thread.sleep(LIST_IMG_TO_PROCESS_THREAD_YELD_TIME);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private static Comparator<ImageContainerText> IMAGE_CONTAINER_COMPARATOR = new Comparator<ImageContainerText>() {

		@Override
		public int compare(ImageContainerText o1, ImageContainerText o2) {
			return -(o1.getWidth() * o1.getHeight()) + (o2.getWidth() * o2.getHeight());
		}};
	public static void createImageToTextPreparer() {
		
		for (int i = 0; i < IMAGE_TO_PROCESS_LIST.length; ++i) {
			final int pos = i;
			new Thread() {
				@Override
				public void run() {
					prepareImageToProcess(IMAGE_TO_PROCESS_LIST[pos]);
					jobFinish[pos] = true;
				}
			}.start();
			
		}
	}
	
	public static void newThreadJob() {
		jobFinish = new boolean[LIST_IMG_TO_PROCESS_COUNT];
	}
	
	public static boolean isConverImageToBufferFinish() {
		boolean result = true;
		for (boolean b: jobFinish) {
			result = result && b;
		}
		if (result)
			Logger.log("No More Texture To Load");
		return result;
	}

	@Override
	public ImageContainer getImageContainer(Object imageData) {
		RawPCXImage pcx = (RawPCXImage) imageData;
		
		PCXHeader header = null;

    	byte[] data = pcx.getData();
        
        try {
			header = new PCXHeader(data);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        int width = header.xmax - header.xmin + 1;
        int height = header.ymax - header.ymin + 1;
        
        ImageContainerText result = new ImageContainerText(pcx , width, height);
        addToImageToProcess(result);
		return result;
			
			
	}
}
