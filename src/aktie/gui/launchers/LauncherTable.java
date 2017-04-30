package aktie.gui.launchers;

import java.util.List;

import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import aktie.data.Launcher;
import aktie.gui.SWTApp;
import aktie.gui.table.AktieTable;
import aktie.gui.table.AktieTableCellLabelProvider;
import aktie.gui.table.AktieTableViewerColumn;

public class LauncherTable extends AktieTable<List<Launcher>, Launcher>
{
    public LauncherTable ( Composite composite, SWTApp app )
    {
        super ( composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI );

        setContentProvider ( new LauncherContentProvider() );

        AktieTableViewerColumn<List<Launcher>, Launcher> column;

        column = addColumn ( "Extension", 100, new LauncherColumnExtension() );
        getTableViewer().setSortColumn ( column, false );

        addColumn ( "Launcher", 300, new LauncherColumnProgram() );
    }

    private class LauncherColumnExtension extends AktieTableCellLabelProvider<Launcher>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                Launcher launcher = Launcher.class.cast ( cell.getElement() );
                cell.setText ( launcher.getExtension() );
            }

            catch ( ClassCastException e )
            {
                cell.setText ( "" );
            }

        }

        @Override
        public int compare ( Object e1, Object e2, boolean reverse )
        {
            if ( e1 == null || e2 == null )
            {
                return 0;
            }

            try
            {
                Launcher launcher1 = Launcher.class.cast ( e1 );
                Launcher launcher2 = Launcher.class.cast ( e2 );
                int comp = launcher1.getExtension().compareToIgnoreCase ( launcher2.getExtension() );

                if ( reverse && comp != 0 )
                {
                    return -comp;
                }

                return comp;
            }

            catch ( ClassCastException e )
            {
                System.err.println ( e.toString() );
                return 0;
            }

        }

    }

    private class LauncherColumnProgram extends AktieTableCellLabelProvider<Launcher>
    {
        @Override
        public void update ( ViewerCell cell )
        {
            try
            {
                Launcher launcher = Launcher.class.cast ( cell.getElement() );
                cell.setText ( launcher.getExtension() );
            }

            catch ( ClassCastException e )
            {
                cell.setText ( "" );
            }

        }

        @Override
        public int compare ( Object e1, Object e2, boolean reverse )
        {
            if ( e1 == null || e2 == null )
            {
                return 0;
            }

            try
            {
                Launcher launcher1 = Launcher.class.cast ( e1 );
                Launcher launcher2 = Launcher.class.cast ( e2 );
                int comp = launcher1.getPath().compareToIgnoreCase ( launcher2.getPath() );

                if ( reverse && comp != 0 )
                {
                    return -comp;
                }

                return comp;
            }

            catch ( ClassCastException e )
            {
                System.err.println ( e.toString() );
                return 0;
            }

        }

    }

}
