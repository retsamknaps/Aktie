package aktie.gui.subtree;

import java.util.List;

public interface SubTreeEntityDBInterface
{

    /**
        Save a new element and set the id value as needed.
    */
    public void saveEntity ( SubTreeEntity e );

    public void saveAll ( List<SubTreeEntity> lst );

    public List<SubTreeEntity> getEntities ( int id );

    public void deleteElement ( SubTreeEntity e );

}
