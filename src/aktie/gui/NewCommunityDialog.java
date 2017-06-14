package aktie.gui;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.List;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;

public class NewCommunityDialog extends Dialog
{
    private Text name;
    private Text description;
    private Text txtUse;
    private SWTApp app;
    private Button btnPublic;
    private Button btnPrivateButName;
    private Button btnPrivate;
    private List identList;
    private ListViewer identListViewer;
    private IdentListProvider listContentProvider;

    private class IdentListProvider implements IStructuredContentProvider
    {
        public String id[];
        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged ( Viewer arg0, Object arg1, Object arg2 )
        {
        }

        @SuppressWarnings ( "rawtypes" )
        @Override
        public Object[] getElements ( Object arg0 )
        {
            if ( arg0 instanceof java.util.List )
            {
                java.util.List l = ( java.util.List ) arg0;
                Object r[] = new Object[l.size()];
                id = new String[l.size()];
                int idx = 0;
                Iterator it = l.iterator();

                while ( it.hasNext() )
                {
                    r[idx] = it.next();
                    id[idx] = ( String ) r[idx];
                    idx++;
                }

                return r;
            }

            if ( arg0 instanceof SWTApp )
            {
                SWTApp a = ( SWTApp ) arg0;

                if ( a.getNode() != null )
                {
                    Index i = a.getNode().getIndex();
                    CObjList idlst = i.getMyIdentities();
                    Object r[] = new Object[idlst.size()];
                    id = new String[idlst.size()];

                    for ( int c = 0; c < idlst.size(); c++ )
                    {
                        try
                        {
                            CObj co = idlst.get ( c );
                            r[c] = co.getDisplayName();
                            id[c] = co.getId();
                        }

                        catch ( IOException e )
                        {
                            e.printStackTrace();
                        }

                    }

                    idlst.close();
                    return r;
                }

            }

            return null;
        }

    }

    /**
        Create the dialog.
        @param parentShell
    */
    public NewCommunityDialog ( Shell parentShell, SWTApp a )
    {
        super ( parentShell );
        setShellStyle ( getShellStyle() | SWT.RESIZE );
        app = a;
    }

    private void selectIdentity ( String id )
    {
        if ( listContentProvider != null && identList != null && !identList.isDisposed() )
        {
            int sel = 0;
            String idl[] = listContentProvider.id;

            for ( int c = 0; c < idl.length; c++ )
            {
                if ( idl[c].equals ( id ) )
                {
                    sel = c;
                }

            }

            identList.select ( sel );
        }

    }

    private String selectedid;
    private Button btnBlogModeonly;

    public void open ( String selid )
    {
        selectedid = selid;
        showMyIdents();
        selectIdentity ( selid );
        super.open();
    }

    private void populateIdentities ( ListViewer lv )
    {
        lv.setLabelProvider ( new ILabelProvider()
        {
            @Override
            public void addListener ( ILabelProviderListener arg0 )
            {
            }

            @Override
            public void dispose()
            {
            }

            @Override
            public boolean isLabelProperty ( Object arg0, String arg1 )
            {
                return false;
            }

            @Override
            public void removeListener ( ILabelProviderListener arg0 )
            {
            }

            @Override
            public Image getImage ( Object arg0 )
            {
                return null;
            }

            @Override
            public String getText ( Object arg0 )
            {
                return ( String ) arg0;
            }

        } );

        listContentProvider = new IdentListProvider();
        lv.setContentProvider ( listContentProvider );
    }

    @Override
    protected void configureShell ( Shell shell )
    {
        super.configureShell ( shell );
        shell.setText ( "New Community" );
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
        new Label ( container, SWT.NONE );

        Composite composite = new Composite ( container, SWT.NONE );
        composite.setLayout ( new RowLayout ( SWT.VERTICAL ) );
        composite.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, false, false, 1, 1 ) );

        btnPublic = new Button ( composite, SWT.RADIO );
        btnPublic.setSelection ( true );
        btnPublic.setText ( "Public" );

        btnPrivateButName = new Button ( composite, SWT.RADIO );
        btnPrivateButName.setText ( "Private access, name displayed publicly as Locked Community" );

        btnPrivate = new Button ( composite, SWT.RADIO );
        btnPrivate.setText ( "Private access, name hidden to public" );
        new Label ( container, SWT.NONE );

        btnBlogModeonly = new Button ( container, SWT.CHECK );
        btnBlogModeonly.setText ( "Blog Mode (Only you can add posts and files)" );

        Label lblName = new Label ( container, SWT.NONE );
        lblName.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblName.setText ( "Name" );

        name = new Text ( container, SWT.BORDER );
        name.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblDescription = new Label ( container, SWT.NONE );
        lblDescription.setLayoutData ( new GridData ( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblDescription.setText ( "Description" );

        description = new Text ( container, SWT.BORDER );
        description.setLayoutData ( new GridData ( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblCreator_1 = new Label ( container, SWT.NONE );
        lblCreator_1.setLayoutData ( new GridData ( SWT.RIGHT, SWT.FILL, false, true, 1, 1 ) );
        lblCreator_1.setText ( "Creator" );

        identListViewer = new ListViewer ( container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE );
        identList = identListViewer.getList();
        GridData gd_identList = new GridData ( SWT.FILL, SWT.FILL, false, true, 1, 1 );
        gd_identList.heightHint = 50;
        identList.setLayoutData ( gd_identList );
        populateIdentities ( identListViewer );

        showMyIdents();

        if ( selectedid != null )
        {
            selectIdentity ( selectedid );
        }

        else
        {
            identList.select ( 0 );
        }

        new Label ( container, SWT.NONE );

        txtUse = new Text ( container, SWT.BORDER | SWT.WRAP );
        txtUse.setEditable ( false );
        txtUse.setText ( "We recommend creating communities with your \"anon\" identity.\n"
                         + "Then you can grant your named identities membership as you wish.\n"
                       );
        txtUse.setLayoutData ( new GridData ( SWT.FILL, SWT.FILL, true, false, 1, 1 ) );

        return container;
    }

    private void showMyIdents()
    {
        if ( app != null )
        {
            if ( identListViewer != null && !identList.isDisposed() )
            {
                identListViewer.setInput ( app );
            }

        }

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

    @Override
    protected void okPressed()
    {
        String sname = name.getText();
        String desc = description.getText();
        String scope = CObj.SCOPE_PRIVATE;
        String blogmode = "false";

        boolean pubname = false;

        if ( btnPublic.getSelection() )
        {
            scope = CObj.SCOPE_PUBLIC;
            pubname = true;
        }

        if ( btnPrivateButName.getSelection() )
        {
            pubname = true;
        }

        if ( btnBlogModeonly.getSelection() )
        {
            blogmode = "true";
        }

        CObj c = new CObj();
        c.setType ( CObj.COMMUNITY );
        c.pushPrivate ( CObj.NAME, sname );
        c.pushPrivate ( CObj.DESCRIPTION, desc );
        c.pushString ( CObj.BLOGMODE, blogmode );

        if ( identList.getSelectionCount() == 1 )
        {
            int idx = identList.getSelectionIndex();
            String id = listContentProvider.id[idx];

            if ( id != null )
            {
                c.pushString ( CObj.CREATOR, id );
                c.pushString ( CObj.SCOPE, scope );

                if ( pubname )
                {
                    c.pushString ( CObj.NAME, sname );
                    c.pushString ( CObj.DESCRIPTION, desc );

                    if ( CObj.SCOPE_PRIVATE.equals ( scope ) )
                    {
                        c.pushString ( CObj.NAME_IS_PUBLIC, "true" );
                    }

                }

                if ( app != null )
                {
                    app.getNode().enqueue ( c );
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
        return new Point ( 500, 450 );
    }

    protected Button getBtnPublic()
    {
        return btnPublic;
    }

    protected Button getBtnPrivateButName()
    {
        return btnPrivateButName;
    }

    protected Button getBtnPrivate()
    {
        return btnPrivate;
    }

    public List getIdentList()
    {
        return identList;
    }

    public Button getBtnBlogModeonly()
    {
        return btnBlogModeonly;
    }

}
