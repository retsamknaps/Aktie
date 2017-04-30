package aktie.gui;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

//import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aktie.data.CObj;
import aktie.gui.table.AktieTableContentProvider;
import aktie.index.CObjList;

/**
    Extend AktieTableContentProvider<CObjList, CObjListGetter> for the sake of compatibility
    with AktieTable<CObjList, CObjListGetter>.
*/
public class CObjContentProvider extends AktieTableContentProvider<CObjList, CObjListGetter>
{

    private List<CObjElement> rowList;

    public CObjContentProvider()
    {
        rowList = new ArrayList<CObjElement>();
    }

    public void clear()
    {
        rowList.clear();
    }

    public List<CObj> getCObjList()
    {
        List<CObj> r = new LinkedList<CObj>();

        for ( CObjElement e : rowList )
        {
            r.add ( e.getCObj() );
        }

        return r;
    }

    public void addCObj ( CObj f )
    {
        CObjElement ne = new CObjElement ( f );

        if ( !rowList.contains ( ne ) )
        {
            rowList.add ( ne );
        }

    }

    public void removeElement ( CObjElement e )
    {
        rowList.remove ( e );
    }

    @Override
    public void dispose()
    {

    }

    @Override
    public void inputChanged ( Viewer arg0, Object arg1, Object arg2 )
    {
    }

    @Override
    public CObjListGetter[] getElements ( Object a )
    {
        return rowList.toArray ( new CObjListGetter[rowList.size()] );
    }

}
