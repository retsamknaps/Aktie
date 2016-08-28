package aktie.gui.launchers;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

import aktie.data.Launcher;

public class ProgramColumnLabelProvider extends CellLabelProvider
{

    @Override
    public void update ( ViewerCell cell )
    {
        if ( cell != null && cell.getElement()  != null )
        {
            Launcher l = ( Launcher ) cell.getElement();
            cell.setText ( l.getPath() );
        }

    }

}
