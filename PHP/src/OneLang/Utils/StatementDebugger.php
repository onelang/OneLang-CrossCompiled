<?php

namespace OneLang\Utils\StatementDebugger;

use OneLang\One\AstTransformer\AstTransformer;
use OneLang\One\Ast\Statements\Statement;
use OneLang\Utils\TSOverviewGenerator\TSOverviewGenerator;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Types\Field;

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
