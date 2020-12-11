import Link from "next/link";
import {Button} from "react-bootstrap";
import React from "react";

export default function LinkButton({text, href, variant}) {
    return (
        <Link href={href}>
            <Button
                className={"m-1"}
                variant={variant}
            >
                {text}
            </Button>
        </Link>
    )
}