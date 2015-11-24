package aktie.gui;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;

import aktie.data.CObj;
import aktie.data.DirectoryShare;

import org.eclipse.swt.widgets.Label;

public class DownloadToShareDialog extends Dialog
{
    private Text txtWhichShareDirectory;
    private Combo comboShare;
    private ComboViewer comboShareViewer;
    private SWTApp app;

    /**
        Create the dialog.
        @param parentShell
    */
    public DownloadToShareDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = a;
    }

    private List<DirectoryShare> inputList;
    private IStructuredSelection downloadList;
    private boolean doPreview;
    private Label lblFilesSelected;

    private void setInput ( IStructuredSelection sel, boolean prv )
    {
        downloadList = sel;
        doPreview = prv;

        if ( inputList != null && comboShare != null && !comboShare.isDisposed() )
        {
            comboShareViewer.setInput ( inputList );
            lblFilesSelected.setText ( "Files selected: " + downloadList.size() );
        }

    }

    public void setShares ( List<DirectoryShare> lst )
    {
        inputList = lst;
    }

    public void open ( IStructuredSelection sel, boolean prv )
    {
        setInput ( sel, prv );
        super.open();
    }

    /**
        Create contents of the dialog.
        @param parent
    */
    @Override
    protected Control createDialogArea ( Composite parent )
    {
        Composite container = ( Composite ) super.createDialogArea ( parent );
        container.setLayout ( new GridLayout ( 1, false ) );

        txtWhichShareDirectory = new Text ( container, SWT.BORDER );
        txtWhichShareDirectory.setEditable ( false );
        txtWhichShareDirectory.setText ( "Select the share directory where to download the files" );
        txtWhichShareDirectory.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        lblFilesSelected = new Label ( container, SWT.NONE );
        lblFilesSelected.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );
        lblFilesSelected.setText ( "Files selected: " );

        comboShareViewer = new ComboViewer ( container, SWT.NONE | SWT.READ_ONLY );
        comboShareViewer.setContentProvider ( new DirectoryShareContentProvider() );
        comboShareViewer.setLabelProvider ( new DirectoryShareLabelProvider() );
        comboShare = comboShareViewer.getCombo();
        comboShare.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        setInput ( downloadList, doPreview );

        return container;
    }

    /**
        Create contents of the button bar.
        @param parent
    */
    @Override
    protected void createButtonsForButtonBar ( Composite parent )
    {
        createButton ( parent, IDialogConstants.OK_ID, "Download",
                       true );
        createButton ( parent, IDialogConstants.CANCEL_ID,
                       IDialogConstants.CANCEL_LABEL, false );
    }

    @SuppressWarnings ( "rawtypes" )
    @Override
    protected void okPressed()
    {
        IStructuredSelection ssel = ( IStructuredSelection ) comboShareViewer.getSelection();
        Iterator i = ssel.iterator();

        if ( i.hasNext() )
        {
            DirectoryShare ds = ( DirectoryShare ) i.next();

            if ( ds != null )
            {
                Iterator i2 = downloadList.iterator();

                while ( i2.hasNext() )
                {
                    Object selo = i2.next();

                    if ( selo instanceof CObjListArrayElement )
                    {
                        CObjListArrayElement ae = ( CObjListArrayElement ) selo;
                        CObj fr = ae.getCObj();

                        String sharename = ds.getShareName();
                        fr.pushString ( CObj.SHARE_NAME, sharename );

                        if ( doPreview )
                        {
                            app.downloadPreview ( fr );
                        }

                        else
                        {
                            app.downloadLargeFile ( fr );
                        }

                    }

                }

            }

        }

        super.okPressed();
    }

    /**
        Return the initial size of the dialog.
    */
    @Override
    protected Point getInitialSize()
    {
        return new Point ( 450, 169 );
    }

    public Combo getComboShare()
    {
        return comboShare;
    }

    public ComboViewer getComboShareViewer()
    {
        return comboShareViewer;
    }

    public Label getLblFilesSelected()
    {
        return lblFilesSelected;
    }

}
