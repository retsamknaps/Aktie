package aktie.gui;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class LocalFileColumnLabelProvider extends  StyledCellLabelProvider
{

    private Index index;

    public LocalFileColumnLabelProvider()
    {

    }

    public void setIndex ( Index i )
    {
        index = i;
    }

    @Override
    public void update ( ViewerCell cell )
    {

        CObjListGetter o = ( CObjListGetter ) cell.getElement();

        CObj pst = o.getCObj();

        if ( pst != null )
        {

            String lf = pst.getPrivate ( CObj.LOCALFILE );

            if ( lf == null )
            {

                String comid = pst.getString ( CObj.COMMUNITYID );
                String wdig = pst.getString ( CObj.FILEDIGEST );
                String pdig = pst.getString ( CObj.FRAGDIGEST );

                if ( comid != null && wdig != null && pdig != null )
                {

                    CObjList clst = index.getMyHasFiles ( comid, wdig, pdig );

                    if ( clst.size() > 0 )
                    {
                        try
                        {
                            CObj hr = clst.get ( 0 );
                            lf = hr.getPrivate ( CObj.LOCALFILE );

                            if ( lf != null )
                            {
                                pst.pushPrivate ( CObj.LOCALFILE, lf );
                                cell.setText ( lf );
                            }

                        }

                        catch ( Exception e )
                        {
                        }

                    }

                    clst.close();

                }

            }

            else
            {
                cell.setText ( lf );
            }

            Long nv = pst.getPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS );

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
