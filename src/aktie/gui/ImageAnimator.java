package aktie.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

public class ImageAnimator implements Runnable
{
    private SWTApp app;
    //event.gc.setAntialias ( SWT.ON );
    //event.gc.drawImage ( image, 0, 0,
    //                     image.getBounds().width, image.getBounds().height,
    //                     imagex, imagey, sw, sh );
    private Canvas imageCanvas;
    private boolean stop;
    private ImageLoader imgLoader;
    private int sw, sh;
    private Image image;
    private int idx = 0;
    private long nextframe;

    public ImageAnimator ( SWTApp app )
    {
        this.app = app;
        Thread t = new Thread ( this );
        t.start();
    }

    public void create()
    {
        imageCanvas = new Canvas ( app.getPostText(), SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED );
        imageCanvas.addPaintListener ( new PaintListener()
        {
            @Override
            public void paintControl ( PaintEvent e )
            {
                paintImage ( e.gc );
            }

        } );

        imageCanvas.addMouseListener ( new MouseListener()
        {
            @Override
            public void mouseDoubleClick ( MouseEvent e )
            {
                app.togglePreviewResize();
                app.getPostText().redraw();
                app.getPostText().setSelection ( 0, 0 );
            }

            @Override
            public void mouseDown ( MouseEvent e )
            {
            }

            @Override
            public void mouseUp ( MouseEvent e )
            {
            }

        } );

    }

    private synchronized void paintImage ( GC gc )
    {
        Image img = image;

        if ( imgLoader != null &&
                img != null && !img.isDisposed() )
        {

            // Set up the offscreen gc
            Image tmpimg = new Image ( app.getShell().getDisplay(), imageCanvas.getBounds() );
            GC gcImage = new GC ( tmpimg );

            ImageData id = imgLoader.data[idx];
            gcImage.setAntialias ( SWT.ON );
            Image frame = new Image ( Display.getDefault(), id );
            gcImage.drawImage ( img, 0, 0 );
            gcImage.drawImage ( frame, 0, 0, frame.getBounds().width,
                                frame.getBounds().height, 0, 0, imageCanvas.getBounds().width,
                                imageCanvas.getBounds().height );

            image = tmpimg;
            img.dispose();
            frame.dispose();

            gc.drawImage ( tmpimg, 0, 0 );
            gcImage.dispose();
        }

    }

    //        private synchronized void overlayImage ( Image tmp, GC g )
    //        {
    //
    //            if ( imgLoader != null )
    //            {
    //              Image bkimg = image;
    //                ImageData id = imgLoader.data[idx];
    //                g.setAntialias ( SWT.ON );
    //                Image img = new Image ( Display.getDefault(), id );
    //                g.drawImage ( bkimg, 0, 0 );
    //                g.drawImage ( img, 0, 0, img.getBounds().width,
    //                              img.getBounds().height, 0, 0, sw, sh );
    //                image = tmp;
    //                img.dispose();
    //                bkimg.dispose();
    //            }

    //
    //        }

    public synchronized void stop()
    {
        stop = true;
        notifyAll();
    }

    public synchronized void update ( ImageLoader il, int imgx, int imgy, int w, int h )
    {
        if ( il != null )
        {
            if ( il != imgLoader || sw != w || sh != h )
            {
                imgLoader = il;
                idx = 0;
                nextframe = System.currentTimeMillis();

                if ( image != null )
                {
                    if ( !image.isDisposed() )
                    {
                        image.dispose();
                    }

                }

                Display display = Display.getDefault();
                image = new Image ( display, w, h );
            }

            sw = w;
            sh = h;
            imageCanvas.setLocation ( imgx, imgy );
            imageCanvas.setSize ( w, h );
            imageCanvas.redraw();
        }

        else
        {
            imgLoader = null;

            if ( image != null )
            {
                if ( !image.isDisposed() )
                {
                    image.dispose();
                }

            }

            imageCanvas.setSize ( 0, 0 );
            app.getPostText().redraw();

        }

        notifyAll();

    }

    private synchronized void incrIndex()
    {
        if ( imgLoader != null )
        {
            idx++;
            idx = idx % imgLoader.data.length;
        }

    }

    private synchronized void nextFrame()
    {
        if ( app.getShell() != null && app.getShell().isDisposed() )
        {
            stop = true;
            return;
        }

        long ct = System.currentTimeMillis();
        long delay = 100L;

        if ( imgLoader == null )
        {
            delay = 10000L;
        }

        else
        {
            if ( imgLoader.data.length <= 1 || image == null )
            {
                delay = 10000L;
            }

            else
            {
                if ( ct >= nextframe )
                {
                    final ImageData id = imgLoader.data[idx];
                    int delayTime = Math.max ( 50, 10 * id.delayTime );
                    //id.
                    nextframe = nextframe + delayTime;

                    Display display = Display.getDefault();
                    display.asyncExec ( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            imageCanvas.redraw();
                            incrIndex();
                        }

                    } );

                }

                delay = Math.max ( 5, nextframe - ct );
            }

        }

        try
        {
            wait ( delay );
        }

        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

    }

    @Override
    public void run()
    {
        while ( !stop )
        {
            nextFrame();
        }

    }

}
