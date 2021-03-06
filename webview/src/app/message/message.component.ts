import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Message} from '../models/Message';
import {environment} from '../../environments/environment';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-message',
  templateUrl: './message.component.html',
  styleUrls: ['./message.component.css']
})
export class MessageComponent implements OnInit {

  constructor(private http: HttpClient, private snackBar: MatSnackBar, private route: ActivatedRoute) {
  }

  showTemplateList = true;
  loadMessageId;

  message: Message = {
    topic: '',
    properties: [],
    content: '',
    sender: '',
    title: '',
    links: [],
    starttime: '',
    isSent: false,
    endtime: '',
    attachment: '',
    logoAttachment: '',
    locationData: null,
    messageDisplayProperties: {},
  };

  sendMessage(message: Message): void {
    if (message != null) {
      this.http.post(environment.backendApiPath + '/message', message, {}).subscribe(
        value => {
          this.snackBar.open('Successful Send!', '', {duration: 1000});
          this.clearMessage();
        },
        error => {
          this.snackBar.open('Could not send message!', '', {
            duration: 1000,
            panelClass: ['alert', 'alert-danger', 'text-center', 'text-danger']
          });
        }
      );
    } else {
      this.snackBar.open('Some message inputs are invalid!', '', {
        duration: 1000,
        panelClass: ['alert', 'alert-danger', 'text-center', 'text-danger']
      });
    }
  }

  toggleTemplateList(): void {
    this.showTemplateList = !this.showTemplateList;
  }

  loadTemplate($event: Message): void {
    this.message = $event;
  }

  ngOnInit(): void {
    if (this.route.snapshot.queryParams.messageId != null) {
      this.loadMessageId = this.route.snapshot.queryParams.messageId;
      this.http.get<Message>(environment.backendApiPath + '/message/' + this.loadMessageId)
        .subscribe(
          retrievedMessage => {
            this.message = retrievedMessage;
          },
          error => {
            console.log(error);
          }
        );
    }
  }

  clearMessage(): void {
    this.message = {
      topic: '',
      properties: [],
      content: '',
      sender: '',
      title: '',
      links: [],
      starttime: '',
      endtime: '',
      isSent: false,
      attachment: '',
      locationData: null,
      logoAttachment: '',
      messageDisplayProperties: {}
    };
  }
}
