package aktie.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;

public class I2PSettingsDialog extends Dialog
{
    private Text inboundLen;
    private Text outboundLen;
    private Text inboundQuant;
    private Text outboundQuant;
    private Text hostTxt;
    private Text portTxt;
    private Properties i2pProps;
    private File propFile;
    private SWTApp app;

    /**
        Create the dialog.
        @param parentShell
    */
    public I2PSettingsDialog ( Shell parentShell, SWTApp p, File propfile )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        propFile = propfile;
        app = p;
        getI2PProps();
    }

    /**
        Create contents of the dialog.
        @param parent
    */
    @Override
    protected Control createDialogArea ( Composite parent )
    {
        Composite container = ( Composite ) super.createDialogArea ( parent );
        container.setLayout ( new GridLayout ( 2, false ) );

        Label lblInboundLength = new Label ( container, SWT.NONE );
        lblInboundLength.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblInboundLength.setText ( "Inbound Length" );

        inboundLen = new Text ( container, SWT.BORDER );
        inboundLen.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblOutboundLength = new Label ( container, SWT.NONE );
        lblOutboundLength.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblOutboundLength.setText ( "Outbound Length" );

        outboundLen = new Text ( container, SWT.BORDER );
        outboundLen.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblInboundQuantity = new Label ( container, SWT.NONE );
        lblInboundQuantity.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblInboundQuantity.setText ( "Inbound Quantity" );

        inboundQuant = new Text ( container, SWT.BORDER );
        inboundQuant.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblOutboundQuantity = new Label ( container, SWT.NONE );
        lblOutboundQuantity.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblOutboundQuantity.setText ( "Outbound Quantity" );

        outboundQuant = new Text ( container, SWT.BORDER );
        outboundQuant.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblHostTxt = new Label ( container, SWT.NONE );
        lblHostTxt.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblHostTxt.setText ( "I2P Router host" );

        hostTxt = new Text ( container, SWT.BORDER );
        hostTxt.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblPortTxt = new Label ( container, SWT.NONE );
        lblPortTxt.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblPortTxt.setText ( "I2P Router port" );

        portTxt = new Text ( container, SWT.BORDER );
        portTxt.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        setFromProps();

        return container;
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                       true );
        createButton ( parent, IDialogConstants.CANCEL_ID,
                       IDialogConstants.CANCEL_LABEL, false );
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 242 );
    }

    public int open ( )
    {
        int r = super.open();
        setFromProps();
        return r;
    }

    private void setFromProps()
    {
        if ( i2pProps != null )
        {
            String tmp = i2pProps.getProperty ( "inbound.length" );

            if ( tmp != null && inboundLen != null && !inboundLen.isDisposed() )
            {
                inboundLen.setText ( tmp );
            }

            tmp = i2pProps.getProperty ( "inbound.quantity" );

            if ( tmp != null && inboundQuant != null && !inboundQuant.isDisposed() )
            {
                inboundQuant.setText ( tmp );
            }

            tmp = i2pProps.getProperty ( "outbound.length" );

            if ( tmp != null && outboundLen != null && !outboundLen.isDisposed() )
            {
                outboundLen.setText ( tmp );
            }

            tmp = i2pProps.getProperty ( "outbound.quantity" );

            if ( tmp != null && outboundQuant != null && !outboundQuant.isDisposed() )
            {
                outboundQuant.setText ( tmp );
            }

            tmp = i2pProps.getProperty ( "i2cp.tcp.host" );

            if ( tmp != null && hostTxt != null && !hostTxt.isDisposed() )
            {
                hostTxt.setText ( tmp );
            }

            tmp = i2pProps.getProperty ( "i2cp.tcp.port" );

            if ( tmp != null && portTxt != null && !portTxt.isDisposed() )
            {
                portTxt.setText ( tmp );
            }

        }

    }

    private void saveI2PProps()
    {
        try
        {
            try
            {
                String num = Integer.toString (
                                 Integer.valueOf ( inboundLen.getText() ) );
                i2pProps.setProperty ( "inbound.length", num );
            }

            catch ( Exception e )
            {
            }

            try
            {
                String num = Integer.toString (
                                 Integer.valueOf ( inboundQuant.getText() ) );
                i2pProps.setProperty ( "inbound.quantity", num );
            }

            catch ( Exception e )
            {
            }

            try
            {
                String num = Integer.toString (
                                 Integer.valueOf ( outboundLen.getText() ) );
                i2pProps.setProperty ( "outbound.length", num );
            }

            catch ( Exception e )
            {
            }

            try
            {
                String num = Integer.toString (
                                 Integer.valueOf ( outboundQuant.getText() ) );
                i2pProps.setProperty ( "outbound.quantity", num );
            }

            catch ( Exception e )
            {
            }

            try
            {
                String num = String.valueOf ( hostTxt.getText() );
                i2pProps.setProperty ( "i2cp.tcp.host", num );
            }

            catch ( Exception e )
            {
            }

            try
            {
                String num = Integer.toString (
                                 Integer.valueOf ( portTxt.getText() ) );
                i2pProps.setProperty ( "i2cp.tcp.port", num );
            }

            catch ( Exception e )
            {
            }


            FileOutputStream fos = new FileOutputStream ( propFile );
            i2pProps.store ( fos, "Aktie I2P props" );
            fos.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    private void setDefaults()
    {
        String tmp = i2pProps.getProperty ( "inbound.length" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "inbound.length", "2" );
        }

        tmp = i2pProps.getProperty ( "inbound.quantity" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "inbound.quantity", "3" );
        }

        tmp = i2pProps.getProperty ( "outbound.length" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "outbound.length", "2" );
        }

        tmp = i2pProps.getProperty ( "outbound.quantity" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "outbound.quantity", "3" );
        }

        tmp = i2pProps.getProperty ( "inbound.nickname" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "inbound.nickname", "AKTIE" );
        }

        tmp = i2pProps.getProperty ( "i2cp.tcp.host" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "i2cp.tcp.host", "127.0.0.1" );
        }

        tmp = i2pProps.getProperty ( "i2cp.tcp.port" );

        if ( tmp == null )
        {
            i2pProps.setProperty ( "i2cp.tcp.port", "7654" );
        }


    }

    @Override
    protected void okPressed()
    {
        saveI2PProps();
        app.setI2PProps ( i2pProps );
        super.okPressed();
    }

    public Properties getI2PProps()
    {
        i2pProps = new Properties();

        try
        {
            FileInputStream fis = new FileInputStream ( propFile );
            i2pProps.load ( fis );
            fis.close();
        }

        catch ( Exception e )
        {
        }

        setDefaults();
        return i2pProps;
    }

    public Text getInboundLen()
    {
        return inboundLen;
    }

    public Text getOutboundLen()
    {
        return outboundLen;
    }

    public Text getInboundQuant()
    {
        return inboundQuant;
    }

    public Text getOutboundQuant()
    {
        return outboundQuant;
    }

    public Text getHostTxt()
    {
        return hostTxt;
    }

    public Text getPortTxt()
    {
        return portTxt;
    }

}
