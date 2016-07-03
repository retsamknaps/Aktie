package aktie.gui.pm;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import swing2swt.layout.BorderLayout;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
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
import aktie.gui.CObjListPrivDispNameColumnLabelProvider;
import aktie.gui.CObjListPrivateColumnLabelProvider;
import aktie.gui.IdentitySelectedInterface;
import aktie.gui.NewPostDialog;
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
    private SelectIdentityDialog selectDialog;
    private SimpleDateFormat dateformat;

    private SubTreeModel identModel;
    private CObj currentMsgId;

    private String sortPostField;
    private boolean sortPostReverse;
    private SortField.Type sortPostType;

    public PMTab ( Composite parent, int style, SWTApp a )
    {
        super ( parent, style );

        dateformat = new SimpleDateFormat ( "d MMM yyyy HH:mm z" );

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

        tableViewer = new TableViewer ( sashForm_1, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );
        provider = new CObjListContentProvider();
        tableViewer.setContentProvider ( provider );
        table = tableViewer.getTable();
        table.setLinesVisible ( true );
        table.setHeaderVisible ( true );
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

                        if ( msg != null )
                        {
                            identModel.clearBlueMessages ( msg );
                            treeViewer.refresh ( true );
                            Long np = msg.getPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS );

                            if ( np != null && !np.equals ( 0L ) )
                            {
                                msg.pushPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS, 0L );

                                try
                                {
                                    app.getNode().getIndex().index ( msg );
                                }

                                catch ( IOException e1 )
                                {
                                    e1.printStackTrace();
                                }

                            }

                            String pfrom = msg.getString ( CObj.CREATOR );
                            String pto = msg.getPrivate ( CObj.PRV_RECIPIENT );

                            if ( pfrom != null && pto != null )
                            {
                                StringBuilder sb = new StringBuilder();
                                sb.append ( "FROM: " );
                                sb.append ( app.getIdCache().getName ( pfrom ) );
                                sb.append ( "\n" );
                                sb.append ( "TO:   " );
                                sb.append ( app.getIdCache().getName ( pto ) );
                                sb.append ( "\n" );
                                sb.append ( "DATE: " );
                                Long co = msg.getPrivateNumber ( CObj.CREATEDON );

                                if ( co != null )
                                {
                                    sb.append ( dateformat.format ( new Date ( co ) ) );
                                }

                                sb.append ( "\n" );
                                sb.append ( "SUBJ: " );
                                sb.append ( msg.getPrivate ( CObj.SUBJECT ) );
                                sb.append ( "\n======================================\n" );

                                String bdy = msg.getPrivate ( CObj.BODY );

                                if ( bdy != null )
                                {
                                    sb.append ( bdy );
                                }

                                String prttxt = NewPostDialog.formatDisplay ( sb.toString(), false );
                                styledText.setText ( prttxt );
                            }

                        }

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

        TableViewerColumn col0 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col0.getColumn().setText ( "Sender" );
        col0.getColumn().setWidth ( 100 );
        col0.setLabelProvider ( new CObjListPrivDispNameColumnLabelProvider ( ) );
        col0.getColumn().addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docPrivate ( CObj.NAME );

                if ( ns.equals ( sortPostField ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField = ns;
                    sortPostReverse = true;
                    sortPostType = SortedNumericSortField.Type.STRING;
                }

                updateMessageTable();
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
        col2.getColumn().addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docPrivateNumber ( CObj.CREATEDON );

                if ( ns.equals ( sortPostField ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField = ns;
                    sortPostReverse = true;
                    sortPostType = SortedNumericSortField.Type.LONG;
                }

                updateMessageTable();
            }

            @Override
            public void widgetDefaultSelected ( SelectionEvent e )
            {
            }

        } );

        TableViewerColumn col1 = new TableViewerColumn ( tableViewer, SWT.NONE );
        col1.getColumn().setText ( "Subject" );
        col1.getColumn().setWidth ( 300 );
        col1.setLabelProvider ( new CObjListPrivateColumnLabelProvider ( CObj.SUBJECT ) );
        col1.getColumn().addSelectionListener ( new SelectionListener()
        {

            @Override
            public void widgetSelected ( SelectionEvent e )
            {
                String ns = CObj.docPrivate ( CObj.SUBJECT );

                if ( ns.equals ( sortPostField ) )
                {
                    sortPostReverse = !sortPostReverse;
                }

                else
                {
                    sortPostField = ns;
                    sortPostReverse = true;
                    sortPostType = SortedNumericSortField.Type.STRING;
                }

                updateMessageTable();
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
                                msgDialog.open ( co.getId(), i.getId() );
                            }

                            if ( CObj.PRIVIDENTIFIER.equals ( co.getType() ) )
                            {
                                String ft[] = getFromTo ( co );

                                if ( ft != null )
                                {
                                    msgDialog.open ( ft[0], i.getId() );
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

    public void openDialog ( CObj msgIdent )
    {
        if ( msgDialog != null && msgIdent != null )
        {
            String ft[] = getFromTo ( msgIdent );

            if ( ft != null )
            {
                msgDialog.open ( ft[0], ft[1] );
            }

        }

    }

    public void setMessageDialog ( PrivateMessageDialog d )
    {
        msgDialog = d;
    }

    private void updateMessageTable()
    {
        if ( currentMsgId != null )
        {
            Sort s = new Sort();

            if ( sortPostField != null && sortPostType != null )
            {
                if ( SortedNumericSortField.Type.LONG.equals ( sortPostType ) )
                {
                    s.setSort ( new SortedNumericSortField ( sortPostField, sortPostType, sortPostReverse ) );
                }

                else
                {
                    s.setSort ( new SortField ( sortPostField, sortPostType, sortPostReverse ) );
                }

            }

            else
            {
                s.setSort ( new SortedNumericSortField ( CObj.docPrivateNumber ( CObj.CREATEDON ), SortedNumericSortField.Type.LONG, true ) );
            }

            String pid = currentMsgId.getPrivate ( CObj.PRV_MSG_ID );

            if ( pid != null )
            {
                CObjList oldlst = ( CObjList ) tableViewer.getInput();
                CObjList nlst = app.getNode().getIndex().getDecodedPrvMessages ( pid, s );
                tableViewer.setInput ( nlst );

                if ( oldlst != null )
                {
                    oldlst.close();
                }

            }

        }

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
