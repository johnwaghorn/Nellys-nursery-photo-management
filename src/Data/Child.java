package Data;

import Core.Library;
import Core.Settings;
import Core.Taggable;

import java.io.Serializable;

public class Child extends Taggable implements Serializable {

	private static final long serialVersionUID = 3434858481118705418L;

    public Child(String name) {
        super(name);
        Library.addTaggableComponent(this);
    }

    @Override
    public int getType() {
        return Settings.CHILD_TAG;
    }
}
