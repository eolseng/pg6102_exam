import React, {useCallback, useEffect, useRef, useState} from "react";
import {TRIP_BASE_PATH} from "../../lib/trip";
import axios from "axios";
import Spinner from "react-bootstrap/Spinner";
import {Button} from "react-bootstrap";
import TripCard from "../cards/TripCard";

export default function TripContainer() {

    // Uses infinite scrolling, but only works once first page is filled.

    const pageSize = 6;

    const [trips, setTrips] = useState([])
    const [next, setNext] = useState(TRIP_BASE_PATH + "/trips?amount?=" + pageSize)
    const [loading, setLoading] = useState(false)
    const [page, setPage] = useState(0)
    const loader = useRef(null)

    useEffect(() => {
        const options = {
            root: null,
            rootMargin: '0px',
            threshold: 0.25
        }
        const observer = new IntersectionObserver(intersectHandler, options)
        if (loader.current && next) {
            observer.observe(loader.current)
        }
    }, []);

    // Handler on intersect
    const intersectHandler = useCallback((entries) => {
        const target = entries[0]
        if (target.isIntersecting && next) {
            setPage((n) => n + 1)
        }
    }, [])

    useEffect(() => {
        loadMore()
    }, [page])

    // Fetch the next page
    const fetchNextPage = async () => await axios.get(next).then(res => res.data.data)

    // Fetch and add data into array
    const loadMore = () => {
        if (next) {
            setLoading(true)
            fetchNextPage()
                .then(data => {
                    // Update 'next' page path
                    if (data.next) setNext(data.next)
                    else setNext(null)
                    // Add new Blueprints to list
                    setTrips([...trips, ...data.list])
                })
            setLoading(false)
        }
    }

    return (
        <div>
            <h1>Trips:</h1>
            <div className={"d-flex flex-wrap"}>
                {trips.map(trip => <TripCard key={trip.id} trip={trip}/>)}
            </div>
            {loading &&
            <Spinner animation={"border"} role={"status"}>
                <span className={"sr-only"}>Loading...</span>
            </Spinner>}
            {next &&
            <Button ref={loader} onClick={loadMore}>Load more</Button>
            }
        </div>
    )

};