import BookingContainer from "../components/containers/bookingContainer";
import React, {useContext} from "react";
import {UserContext} from "../contexts/UserContext";

export default function Bookings() {

    const {state} = useContext(UserContext)

    if (!state.user) return (
        <h1>Must be signed in to view bookings</h1>
    )
    return(
        <BookingContainer/>
    )
};