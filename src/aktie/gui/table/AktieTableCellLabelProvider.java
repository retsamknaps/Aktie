package aktie.gui.table;

import org.eclipse.jface.viewers.StyledCellLabelProvider;

public abstract class AktieTableCellLabelProvider<T> extends StyledCellLabelProvider
{

    public AktieTableCellLabelProvider()
    {

    }

    public abstract int compare ( Object o1, Object o2, boolean reverse );

}
