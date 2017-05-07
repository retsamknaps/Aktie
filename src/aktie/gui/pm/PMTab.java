package aktie.gui.pm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import swing2swt.layout.BorderLayout;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;

import aktie.data.CObj;
import aktie.gui.IdentitySelectedInterface;
import aktie.gui.SWTApp;
import aktie.gui.SelectIdentityDialog;
import aktie.gui.subtree.SubTreeDragListener;
import aktie.gui.subtree.SubTreeDropListener;
import aktie.gui.subtree.SubTreeEntity;
import aktie.gui.subtree.SubTreeEntityDB;
import aktie.gui.subtree.SubTreeLabelProvider;
import aktie.gui.subtree.SubTreeListener;
import aktie.gui.subtree.SubTreeModel;
import aktie.gui.subtree.SubTreeSorter;
import aktie.index.CObjList;

public class PMTab extends Composite
{

    private PMTable table;
    private Tree tree;
    private TreeViewer treeViewer;
    private StyledText styledText;
    private SWTApp app;
    private PrivateMessageDialog msgDialog;
    private SelectIdentityDialog selectDialog;

    private SubTreeModel identModel;
    private CObj currentMsgId;

    public PMTab ( Composite parent, int style, SWTApp a )
    {
        super ( parent, style );

        app = a;
        setLayout ( new BorderLayout ( 0, 0 ) );

        SashForm sashForm = new SashForm ( this, SWT.NONE );
        sashForm.setLayoutData ( BorderLayout.CENTER );

        treeViewer = new TreeViewer ( sashForm, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );
        tree = treeViewer.getTree();
        treeViewer.addSelectionChangedListener ( new ISelectionChangedListener()
        {
            @SuppressWarnings ( "rawtypes" )
            @Override
            public void selectionChanged ( SelectionChangedEvent s )
            {
                IStructuredSelection sel = ( IStructuredSelection ) s.getSelection();
                Iterator i = sel.iterator();

                if ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof SubTreeEntity )
                    {
                        SubTreeEntity sm = ( SubTreeEntity ) selo;
                        CObj co = identModel.getCObj ( sm.getId() );

                        if ( co != null )
                        {
                            if ( CObj.PRIVIDENTIFIER.equals ( co.getType() ) )
                            {
                                currentMsgId = co;
                                updateMessageTable();
                            }

                        }

                    }

                }

            }

        } );

        Menu menu_5 = new Menu ( tree );
        tree.setMenu ( menu_5 );

        MenuItem reply = new MenuItem ( menu_5, SWT.NONE );
        reply.setText ( "New Message" );
        reply.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                IStructuredSelection sel = ( IStructuredSelection ) treeViewer.getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator i = sel.iterator();

                if ( i.hasNext() )
                {
                    Object selo = i.next();

                    if ( selo instanceof SubTreeEntity )
                    {
                        SubTreeEntity sm = ( SubTreeEntity ) selo;
                        CObj co = identModel.getCObj ( sm.getId() );

                        if ( co != null )
                        {
                            if ( CObj.PRIVIDENTIFIER.equals ( co.getType() ) )
                            {
                                openDialog ( co, null );
                            }

                        }

                    }

                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem newmsg = new MenuItem ( menu_5, SWT.NONE );
        newmsg.setText ( "Send Message To..." );
        newmsg.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                selectDialog.open();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        MenuItem refresh = new MenuItem ( menu_5, SWT.NONE );
        refresh.setText ( "Refresh" );
        refresh.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                CObj u = new CObj();
                u.setType ( CObj.USR_PRVMSG_UPDATE );
                app.getNode().enqueue ( u );
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        SashForm sashForm_1 = new SashForm ( sashForm, SWT.VERTICAL );
        table = new PMTable ( sashForm_1, this );

        Menu menu = new Menu ( tree );
        table.setMenu ( menu );

        MenuItem reply2 = new MenuItem ( menu, SWT.NONE );
        reply2.setText ( "Reply" );
        reply2.addSelectionListener ( new SelectionListener()
        {
            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                if ( currentMsgId != null )
                {
                    openDialog ( currentMsgId, table.getCurrentMessage() );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        Composite composite = new Composite ( sashForm_1, SWT.NONE );
        composite.setLayout ( new BorderLayout ( 0, 0 ) );

        styledText = new StyledText ( composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI );
        styledText.setLayoutData ( BorderLayout.CENTER );
        styledText.setFont ( JFaceResources.getFont ( JFaceResources.TEXT_FONT ) );
        styledText.setEditable ( false );
        styledText.setCaret ( null );

        sashForm_1.setWeights ( new int[] {1, 1} );

        sashForm.setWeights ( new int[] {1, 4} );

        selectDialog = new SelectIdentityDialog ( getShell(), app, new IdentitySelectedInterface()
        {
            @Override
            public void selectedIdentity ( CObj i )
            {
                IStructuredSelection sel = ( IStructuredSelection ) treeViewer.getSelection();

                @SuppressWarnings ( "rawtypes" )
                Iterator it = sel.iterator();

                if ( it.hasNext() )
                {
                    Object selo = it.next();

                    if ( selo instanceof SubTreeEntity )
                    {
                        SubTreeEntity sm = ( SubTreeEntity ) selo;
                        CObj co = identModel.getCObj ( sm.getId() );

                        if ( co != null && i != null )
                        {
                            if ( CObj.IDENTITY.equals ( co.getType() ) )
                            {
                                msgDialog.open ( co.getId(), i.getId(), null );
                            }

                            if ( CObj.PRIVIDENTIFIER.equals ( co.getType() ) )
                            {
                                String ft[] = getFromTo ( co );

                                if ( ft != null )
                                {
                                    msgDialog.open ( ft[0], i.getId(), null );
                                }

                            }

                        }

                    }

                }

            }

        } );

    }

    public static String[] getFromTo ( CObj msgIdent )
    {
        if ( msgIdent != null )
        {
            boolean mine = "true".equals ( msgIdent.getPrivate ( CObj.MINE ) );
            String fid = null;
            String tid = null;

            if ( mine )
            {
                fid = msgIdent.getString ( CObj.CREATOR );
                tid = msgIdent.getPrivate ( CObj.PRV_RECIPIENT );
            }

            else
            {
                fid = msgIdent.getPrivate ( CObj.PRV_RECIPIENT );
                tid = msgIdent.getString ( CObj.CREATOR );
            }

            if ( fid != null && tid != null )
            {
                return new String[] {fid, tid};

            }

        }

        return null;
    }

    public void openDialog ( CObj msgIdent, CObj replytomsg )
    {
        if ( msgDialog != null && msgIdent != null )
        {
            String ft[] = getFromTo ( msgIdent );

            if ( ft != null )
            {
                msgDialog.open ( ft[0], ft[1], replytomsg );
            }

        }

    }

    public void setMessageDialog ( PrivateMessageDialog d )
    {
        msgDialog = d;
    }

    private void updateMessageTable()
    {
        table.searchAndSort();
    }

    public void updateMessages()
    {
        Display.getDefault().asyncExec ( new Runnable()
        {
            @Override
            public void run()
            {
                CObjList clst = null;

                try
                {
                    Set<String> myset = new HashSet<String>();
                    clst = app.getNode().getIndex().getMyIdentities();

                    for ( int c = 0; c < clst.size(); c++ )
                    {
                        myset.add ( clst.get ( c ).getId() );
                    }

                    clst.close();

                    for ( String id : myset )
                    {
                        clst = app.getNode().getIndex().getPrivateMsgIdentForIdentity ( id );

                        for ( int c = 0; c < clst.size(); c++ )
                        {
                            CObj pid = clst.get ( c );
                            update ( pid );
                        }

                        clst.close();
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

                finally
                {
                    if ( clst != null )
                    {
                        try
                        {
                            clst.close();
                        }

                        catch ( Exception e2 )
                        {
                        }

                    }

                }

            }

        } );

    }

    public void init()
    {
        identModel = new SubTreeModel ( app.getNode().getIndex(),
                                        new SubTreeEntityDB ( app.getNode().getSession() ),
                                        SubTreeModel.MESSAGE_TREE, 1 );
        identModel.init();
        treeViewer.setContentProvider ( identModel );
        SubTreeListener stl = new SubTreeListener ( identModel );
        treeViewer.addTreeListener ( stl );
        //identTreeViewer.setLabelProvider();
        treeViewer.setSorter ( new SubTreeSorter() );
        int operations = DND.DROP_COPY | DND.DROP_MOVE;
        Transfer[] transferTypes = new Transfer[] {TextTransfer.getInstance() };

        treeViewer.addDragSupport ( operations, transferTypes ,
                                    new SubTreeDragListener ( treeViewer ) );
        treeViewer.addDropSupport ( operations, transferTypes,
                                    new SubTreeDropListener ( treeViewer, identModel ) );

        TreeViewerColumn tvc1 = new TreeViewerColumn ( treeViewer, SWT.NONE );
        tvc1.getColumn().setText ( "Name" ); //$NON-NLS-1$
        tvc1.getColumn().setWidth ( 200 );
        tvc1.setLabelProvider ( new DelegatingStyledCellLabelProvider (
                                    new SubTreeLabelProvider ( identModel ) ) );

    }

    public void update ( CObj c )
    {
        identModel.update ( c );
        treeViewer.setInput ( "Here is some data" );
        identModel.setCollaspseState ( treeViewer );
        updateMessageTable();
    }

    public Tree getTree()
    {
        return tree;
    }

    public TreeViewer getTreeViewer()
    {
        return treeViewer;
    }

    public SubTreeModel getIdentityModel()
    {
        return identModel;
    }

    public SWTApp getSWTApp()
    {
        return app;
    }

    public CObj getCurrentMessage()
    {
        return currentMsgId;
    }

    public StyledText getStyledText()
    {
        return styledText;
    }

}
