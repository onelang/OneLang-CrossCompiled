<?php

namespace Utils\StatementDebugger;

use One\AstTransformer\AstTransformer;
use One\Ast\Statements\Statement;
use Utils\TSOverviewGenerator\TSOverviewGenerator;
use One\Ast\Expressions\Expression;
use One\Ast\Types\Field;

class StatementDebugger extends AstTransformer {
    public $stmtFilterRegex;
    
    function __construct($stmtFilterRegex) {
        parent::__construct("StatementDebugger");
        $this->stmtFilterRegex = $stmtFilterRegex;
    }
    
    protected function visitExpression($expr) {
        // optimization: no need to process these...
        return null;
    }
    
    protected function visitField($field) {
        //if (field.name === "type" && field.parentInterface.name === "Interface") debugger;
        parent::visitField($field);
    }
    
    protected function visitStatement($stmt) {
        $stmtRepr = TSOverviewGenerator::$preview->stmt($stmt);
        // if (new RegExp(this.stmtFilterRegex).test(stmtRepr))
        //     debugger;
        return parent::visitStatement($stmt);
    }
}
