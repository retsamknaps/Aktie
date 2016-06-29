package aktie.gui.pm;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import swing2swt.layout.BorderLayout;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Tree;

import aktie.data.CObj;
import aktie.gui.CObjListArrayElement;
import aktie.gui.CObjListContentProvider;
import aktie.gui.CObjListDateColumnLabelProvider;
import aktie.gui.CObjListPrivateColumnLabelProvider;
import aktie.gui.SWTApp;
import aktie.gui.subtree.SubTreeDragListener;
import aktie.gui.subtree.SubTreeDropListener;
import aktie.gui.subtree.SubTreeEntity;
import aktie.gui.subtree.SubTreeEntityDB;
import aktie.gui.subtree.SubTreeLabelProvider;
import aktie.gui.subtree.SubTreeListener;
import aktie.gui.subtree.SubTreeModel;
import aktie.gui.subtree.SubTreeSorter;
import aktie.index.CObjList;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;

public class PMTab extends Composite
{

    private Table table;
    private Tree tree;
    private TreeViewer treeViewer;
    private TableViewer tableViewer;
    private StyledText styledText;
    private SWTApp app;
    private PrivateMessageDialog msgDialog;
    private CObjListContentProvider provider;

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
                                String lid = co.getPrivate ( CObj.PRV_MSG_ID );

                                if ( lid != null )
                                {
                                    currentMsgId = co;
                                    CObjList oldlst = ( CObjList ) tableViewer.getInput();
                                    CObjList nlst = app.getNode().getIndex().getDecodedPrvMessages ( lid, null );
                                    tableViewer.setInput ( nlst );

                                    if ( oldlst != null )
                                    {
                                        oldlst.close();
                                    }

                                }

                            }

                        }

                    }

                }

            }

        } );

        Menu menu_5 = new Menu ( tree );
        tree.setMenu ( menu_5 );

        MenuItem reply = new MenuItem ( menu_5, SWT.NONE );
        reply.setText ( "Reply" );
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
                                openDialog ( co );
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

        SashForm sashForm_1 = new SashForm ( sashForm, SWT.VERTICAL );

        tableViewer = new TableViewer ( sashForm_1, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );
        provider = new CObjListContentProvider();
        tableViewer.setContentProvider ( provider );
        table = tableViewer.getTable();
        table.setLinesVisible ( true );
        tableViewer.addSelectionChangedListener ( new ISelectionChangedListener()
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

                    if ( selo instanceof CObjListArrayElement )
                    {
                        CObjListArrayElement selm = ( CObjListArrayElement ) selo;
                        CObj msg = selm.getCObj();
                        String bdy = msg.getPrivate ( CObj.BODY );

                        if ( bdy == null )
                        {
                            bdy = "";
                        }

                        styledText.setText ( bdy );
                    }

                }

            }

        } );

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
                    openDialog ( currentMsgId );
                }

            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn col2 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col2.getColumn().setText ( "Date" );
        col2.getColumn().setWidth ( 100 );
        col2.setLabelProvider ( new CObjListDateColumnLabelProvider ( CObj.CREATEDON, true ) );

        TableViewerColumn col1 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col1.getColumn().setText ( "Subject" );
        col1.getColumn().setWidth ( 300 );
        col1.setLabelProvider ( new CObjListPrivateColumnLabelProvider ( CObj.SUBJECT ) );

        Composite composite = new Composite ( sashForm_1, SWT.NONE );
        composite.setLayout ( new BorderLayout ( 0, 0 ) );

        styledText = new StyledText ( composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI );
        styledText.setLayoutData ( BorderLayout.CENTER );
        styledText.setFont ( JFaceResources.getFont ( JFaceResources.TEXT_FONT ) );
        styledText.setEditable ( false );
        styledText.setCaret ( null );

        sashForm_1.setWeights ( new int[] {1, 1} );

        sashForm.setWeights ( new int[] {1, 4} );

    }

    public void openDialog ( CObj msgIdent )
    {
        if ( msgDialog != null && msgIdent != null )
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
                msgDialog.open ( fid, tid );
            }

        }

    }

    public void setMessageDialog ( PrivateMessageDialog d )
    {
        msgDialog = d;
    }

    public void updateMessages()
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

        updateMessages();

    }

    public void update ( CObj c )
    {
        identModel.update ( c );
        treeViewer.setInput ( "Here is some data" );
        identModel.setCollaspseState ( treeViewer );
    }

    public Tree getTree()
    {
        return tree;
    }

    public TreeViewer getTreeViewer()
    {
        return treeViewer;
    }

    public Table getTable()
    {
        return table;
    }

    public TableViewer getTableViewer()
    {
        return tableViewer;
    }

    public StyledText getStyledText()
    {
        return styledText;
    }

}
