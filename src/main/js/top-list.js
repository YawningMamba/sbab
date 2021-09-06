function Stop(props) {
    return (
        <div className="line_stop">
  <span className="stop_text">
    <span className="stop_id">{props.id}</span>
    <span className="stop_name">{props.name}</span>
  </span>
        </div>
    );
}

class Line extends React.Component {
    constructor(props) {
        super(props);
        this.id = props.id;
        this.stops = props.stops.map((stop, index) => <Stop key={index} id={stop.id} name={stop.name}/>);
        this.defaultStopsClass = "line_stops";
        this.state = {
            stopsVisible: false
        }
    };

    toggleStops() {
        this.setState({
            stopsVisible: !this.state.stopsVisible
        });
    }

    render() {

        let lineStopsClass;
        if (this.state.stopsVisible) {
            lineStopsClass = this.defaultStopsClass + " line_stops_visible";
        } else {
            lineStopsClass = this.defaultStopsClass;
        }

        return (
            <div className="line">
                <div className="line_row" onClick={() => {this.toggleStops()}} >
                    <span className="line_id">{this.id}</span>
                </div>
                <div className={lineStopsClass}>{this.stops}</div>
            </div>
        );
    }
}

function TopList(props) {
    const lineItems = props.data.map((line, index) =>
        <Line key={index} id={line.line} stops={line.stops} />
    );

    return (<div className="top_list">{lineItems}</div>);
}

function NoContent() {
    return (<div className="no_content"><p>Top list available, please come back later!</p></div>);
}

const root = 'root';

fetch(document.URL + "/api/BUS")
    .then(response => {
        if (response.status === 200) {

            response.json().then((data) => {
                ReactDOM.render(
                    <TopList data={data}/>,
                    document.getElementById(root));
            });
        } else {
            ReactDOM.render(
                <NoContent />,
                document.getElementById(root));
        }
    });