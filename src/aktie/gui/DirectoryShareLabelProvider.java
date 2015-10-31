package aktie.gui;

import org.eclipse.jface.viewers.LabelProvider;

import aktie.data.DirectoryShare;

public class DirectoryShareLabelProvider extends LabelProvider
{

    @Override
    public String getText ( Object element )
    {
        DirectoryShare ds = ( DirectoryShare ) element;
        return ds.getShareName();
    }

}
