package com.example.demobot.service;

import com.example.demobot.config.BotConfig;
import com.example.demobot.entity.Ads;
import com.example.demobot.entity.TgUser;
import com.example.demobot.repository.AdsRepository;
import com.example.demobot.repository.TgUserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service

public class TgBot extends TelegramLongPollingBot {

    @Autowired
    private AdsRepository adsRepository;

    @Autowired
    private TgUserRepository userRepository;
    final BotConfig config;
    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /deletedata to delete stored data about yourself\n\n" +
            "Type /help to see this message again\n\n" +
            "Type /settings to change settings for more preferable\n\n";
    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";
    static final String ERROR_TEXT = "Error occurred: ";


    public TgBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String mesText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
//            Проверка на то, отсылает ли сообщение собственник + рассылка непредвиденного сообщения, без автоматизации
            if (mesText.contains("/send") && config.getOwnerId() == chatId) {
                String textToSend = EmojiParser.parseToUnicode(mesText.substring(mesText.indexOf(" ")));
                List<TgUser> users = userRepository.findAll();
                for (TgUser us : users) {
                    prepareAndSendMessage(us.getChatId(), textToSend);
                }
            } else {
                switch (mesText) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    case "/register":
                        register(chatId);
                        break;
                    case "/send":
                        break;
                    default:
                        prepareAndSendMessage(chatId, "Sorry, command was not recognised");
                }
            }
        } else if (update.hasCallbackQuery()) {
//            это и есть ид кнопки (CallbackData), которую мы назначаем кнопкам
            String callbackData = update.getCallbackQuery().getData();
//            Передался ли ид кнопки? у каждого смс также есть свое ид
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(YES_BUTTON)) {
                executeEditMessageText("You pressed YES button", chatId, messageId);

            } else if (callbackData.equals(NO_BUTTON)) {
                executeEditMessageText("You pressed NO button", chatId, messageId);
            }
        }
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
//                Заменяем текст того сообщения, которое мы отослали, когда знаем инд сообщения
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId(Integer.parseInt(String.valueOf(messageId)));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
//        id of the button "yes"
        yesButton.setCallbackData(YES_BUTTON);

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
//        id of the button "no"
        noButton.setCallbackData(NO_BUTTON);

        rowInline.add(yesButton);
        rowInline.add(noButton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush: ");
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        createQuickReplies(message);

        executeMessage(message);

    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private void createQuickReplies(SendMessage message) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("/weather");
        row.add("/get random joke");

        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("/register");
        row.add("/check my data");
        row.add("/delete my data");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            long chatId = message.getChatId();
            Chat chat = message.getChat();
            TgUser userToRegister = new TgUser();
            userToRegister.setUserName(chat.getUserName());
            userToRegister.setFirstName(chat.getFirstName());
            userToRegister.setLastName(chat.getLastName());
            userToRegister.setChatId(chatId);
            userToRegister.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(userToRegister);
            log.info("user saved " + userToRegister);
        }
    }

    @Scheduled(cron = "${cron.scheduler}")
    private void sendAds(){
        List<Ads> adsList = adsRepository.findAll();
        List<TgUser> tgUsers = userRepository.findAll();
        for (Ads ad: adsList){
            for (TgUser us : tgUsers) {
                prepareAndSendMessage(us.getChatId(), ad.getAd());
            }
        }
    }
}
