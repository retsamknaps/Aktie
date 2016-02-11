package aktie.gui;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import aktie.data.CObj;

public class CObjContentProvider implements IStructuredContentProvider
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
        rowList.add ( new CObjElement ( f ) );
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
    public Object[] getElements ( Object a )
    {
        return rowList.toArray();
    }

}
