package aktie.gui.pm;

import org.eclipse.swt.widgets.Composite;
import swing2swt.layout.BorderLayout;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Tree;

import aktie.gui.SWTApp;

import org.eclipse.jface.viewers.TreeViewer;

public class PMTab extends Composite
{

    private Table table;
    private Tree tree;
    private TreeViewer treeViewer;
    private TableViewer tableViewer;
    private StyledText styledText;
    private SWTApp app;

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
