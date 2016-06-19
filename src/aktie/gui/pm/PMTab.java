package aktie.gui.pm;

import org.eclipse.swt.widgets.Composite;
import swing2swt.layout.BorderLayout;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Tree;

import aktie.data.CObj;
import aktie.gui.SWTApp;
import aktie.gui.subtree.SubTreeDragListener;
import aktie.gui.subtree.SubTreeDropListener;
import aktie.gui.subtree.SubTreeEntityDB;
import aktie.gui.subtree.SubTreeLabelProvider;
import aktie.gui.subtree.SubTreeListener;
import aktie.gui.subtree.SubTreeModel;
import aktie.gui.subtree.SubTreeSorter;

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

    private SubTreeModel identModel;

    public PMTab ( Composite parent, int style, SWTApp a )
    {
        super ( parent, style );
        app = a;
        setLayout ( new BorderLayout ( 0, 0 ) );

        SashForm sashForm = new SashForm ( this, SWT.NONE );
        sashForm.setLayoutData ( BorderLayout.CENTER );


        treeViewer = new TreeViewer ( sashForm, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );
        tree = treeViewer.getTree();


        SashForm sashForm_1 = new SashForm ( sashForm, SWT.VERTICAL );

        tableViewer = new TableViewer ( sashForm_1, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );
        table = tableViewer.getTable();

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
