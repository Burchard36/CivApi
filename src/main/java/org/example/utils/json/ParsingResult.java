package org.example.utils.json;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParsingResult {

    protected boolean isInvalidSchema = false;
    protected boolean isInvalidJson = false;
    protected Object data = null;

}
