import React from "react";
import Card from "react-bootstrap/Card";

export default function BlueprintCard({blueprint}) {
    return (
        <Card
            style={{width: '18rem'}}
            className={"m-2"}
            border={"dark"}
        >
            <Card.Body>
                <Card.Title>{blueprint.title}</Card.Title>
                <Card.Subtitle className={"mb-2 text-muted"}>Value: {blueprint.value}</Card.Subtitle>
                <Card.Text>{blueprint.description}</Card.Text>
            </Card.Body>
            <Card.Footer className={"text-left text-muted"}>Id: {blueprint.id}</Card.Footer>
        </Card>
    )

}