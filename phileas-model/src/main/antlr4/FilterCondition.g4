grammar FilterCondition;

@header {package com.mtnfog.philter.model.conditions.parser;}

expression: (
    | 'population' COMPARATOR NUMBER (AND expression)?
    | 'token' COMPARATOR '"' WORD '"' (AND expression)?
    | 'type' COMPARATOR TYPE (AND expression)?
    | 'confidence' COMPARATOR NUMBER (AND expression)?
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
    );

NUMBER: (
        ('0'..'9')+('.' ('0'..'9')+)?
    );

WORD: '"' (~('"' | '\\' | '\r' | '\n') | '\\' ('"' | '\\'))* '"';

WS
    :   [ \t\r\n]+ -> skip
    ;