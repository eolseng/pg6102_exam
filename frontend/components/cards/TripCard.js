import React, {useContext} from "react";
import Card from "react-bootstrap/Card";
import {UserContext} from "../../contexts/UserContext";
import Link from "next/link";
import Button from "react-bootstrap/Button";
import axios from "axios";
import {TRIP_BASE_PATH} from "../../lib/trip";
import {useRouter} from "next/router";

export default function TripCard({trip}) {

    const {state} = useContext(UserContext)

    const router = useRouter()

    function isAdmin() {
        if (state.user) {
            return state.user.roles.includes("ROLE_ADMIN")
        }
    }

    async function cancelTrip() {
        await axios
            .delete(TRIP_BASE_PATH + "/trips/" + trip.id)
            .then(res => {
                res.data.data
            })
            .catch(err => {
                console.error(err)
                return false
            })
        window.location.href = "/"
    }

    return (
        <Card
            style={{width: '30rem'}}
            className={"m-2"}
            border={"dark"}
        >
            <Card.Body>
                <Card.Subtitle className={"mb-2 text-muted"}>
                    <span>Price: {trip.price}</span>
                </Card.Subtitle>
                <Card.Title>{trip.title}</Card.Title>
                <Card.Text>{trip.description}</Card.Text>
            </Card.Body>
            <Card.Footer className={"text-left text-muted"}>
                <p><strong>Location:</strong> {trip.location}</p>
                <p><strong>Start:</strong> {new Date(trip.start).toLocaleString()}</p>
                <p>
                    <strong>Duration:</strong> {trip.duration.days} days, {trip.duration.hours} hours, {trip.duration.minutes} minutes
                </p>
                {state.user &&
                <Link href={"/trip/" + trip.id}>
                    <Button className={"m-1"}>BOOK TRIP</Button>
                </Link>}
                {isAdmin() &&
                <Button className={"m-1"} onClick={cancelTrip}>CANCEL TRIP</Button>}
            </Card.Footer>
        </Card>
    )

}