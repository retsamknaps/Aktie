package aktie.gui;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import aktie.data.CObj;

public class CObjListCachePrivateIdentityLableProvider extends StyledCellLabelProvider
{

    private IdentityCache cache;
    private String key;

    public CObjListCachePrivateIdentityLableProvider ( IdentityCache c, String k )
    {
        key = k;
        cache = c;
    }

    @Override
    public void update ( ViewerCell cell )
    {
        CObjListGetter o = ( CObjListGetter ) cell.getElement();
        CObj co = o.getCObj();

        if ( co != null )
        {
            String id = co.getPrivate ( key );

            if ( id == null )
            {
                id = "";
            }

            else
            {
                id = cache.getName ( id );
            }

            cell.setText ( id );

            Long nv = co.getPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS );

            if ( nv != null && 1L == nv )
            {
                cell.setForeground ( Display.getDefault().getSystemColor ( SWT.COLOR_BLUE ) );
            }

            else
            {
                cell.setForeground ( Display.getDefault().getSystemColor ( SWT.COLOR_WIDGET_FOREGROUND ) );
            }

        }

        else
        {
            cell.setText ( "" );
        }

    }

}
