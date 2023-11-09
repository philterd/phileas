grammar FilterCondition;

@header {package ai.philterd.phileas.model.conditions.parser;}

expression: (
    | 'population' COMPARATOR NUMBER (AND expression)?
    | 'token' COMPARATOR WORD (AND expression)?
    | 'type' COMPARATOR TYPE (AND expression)?
    | 'confidence' COMPARATOR NUMBER (AND expression)?
    | 'context' COMPARATOR WORD (AND expression)?
    | 'classification' COMPARATOR WORD (AND expression)?
    | 'sentiment' COMPARATOR NUMBER (AND expression)?
    );

TYPE: (
    | 'PER' | 'per'
    | 'LOC' | 'loc'
    );

AND: (
    | 'AND'
    | 'and'
    );

COMPARATOR: (
    | '>'
    | '<'
    | '<='
    | '=>'
    | '=='
    | '!='
    | 'startswith'
    | 'is'
    );

NUMBER: (
        ('0'..'9')+('.' ('0'..'9')+)?
    );

WORD: '"' (~('"' | '\\' | '\r' | '\n') | '\\' ('"' | '\\'))* '"';

WS
    :   [ \t\r\n]+ -> skip
    ;