package aktie.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class AktieSplash extends Frame
{
    private static final long serialVersionUID = 1L;

    private BorderLayout borderLayout = new BorderLayout();
    private JLabel imageLabel = new JLabel();
    private JProgressBar progressBar = new JProgressBar ( 0, 100 );


    public AktieSplash ( String nodedir )
    {
        setLayout ( borderLayout );
        setSize ( 100, 100 );
        BufferedImage img = null;

        try
        {
            img = ImageIO.read ( new File ( nodedir + "/img/splash.png" ) );
        }

        catch ( IOException e )
        {
        }

        if ( img != null )
        {
            imageLabel.setIcon ( new ImageIcon ( img ) );
        }

        else
        {
            System.out.println ( "COULD NOT FIND IMAGE" );
        }

        add ( imageLabel, BorderLayout.CENTER );
        add ( progressBar, BorderLayout.SOUTH );
        pack();
        setLocationRelativeTo ( null );
    }


    private void runInEdt ( final Runnable runnable )
    {
        if ( SwingUtilities.isEventDispatchThread() )
        { runnable.run(); }

        else
        { SwingUtilities.invokeLater ( runnable ); }

    }

    public void showScreen()
    {
        runInEdt ( new Runnable()
        {
            public void run()
            {
                setVisible ( true );
            }

        } );

    }

    public void close()
    {
        runInEdt ( new Runnable()
        {
            public void run()
            {
                setVisible ( false );
                dispose();
            }

        } );

    }

    public void setProgress ( final String message, final int progress )
    {
        runInEdt ( new Runnable()
        {
            public void run()
            {
                progressBar.setValue ( progress );

                if ( message == null )
                { progressBar.setStringPainted ( false ); }

                else
                { progressBar.setStringPainted ( true ); }

                progressBar.setString ( message + "..." );
            }

        } );

    }



    public static void main ( String args[] )
    {
        AktieSplash sp = new AktieSplash ( "" );
        sp.showScreen();

        for ( int c = 0; c < 100; c++ )
        {
            sp.setProgress ( "some message", c );

            try
            {
                Thread.sleep ( 500 );
            }

            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }

        }

        sp.close();
    }

}
