package aktie.gui.subtree;

import java.util.LinkedList;
import java.util.List;

public class SubTreeEntityDBTest implements SubTreeEntityDBInterface
{

    private long curId = 0;

    @Override

    public synchronized void saveEntity ( SubTreeEntity e )
    {
        if ( e.getId() == 0 )
        {
            curId++;
            e.setId ( curId );
        }

    }

    @Override
    public List<SubTreeEntity> getEntities()
    {
        return new LinkedList<SubTreeEntity>();
    }

    @Override
    public void deleteElement ( SubTreeEntity e )
    {

    }

    @Override

    public synchronized void saveAll ( List<SubTreeEntity> lst )
    {
        for ( SubTreeEntity e : lst )
        {
            saveEntity ( e );
        }

    }

}
